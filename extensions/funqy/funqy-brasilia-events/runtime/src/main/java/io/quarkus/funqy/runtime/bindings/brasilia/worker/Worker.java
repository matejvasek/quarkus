package io.quarkus.funqy.runtime.bindings.brasilia.worker;

import static io.quarkus.funqy.runtime.bindings.brasilia.worker.Message.CompleteEvent.InvocationResult.InvocationStatus.SUCCEEDED;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.bindings.brasilia.BrasiliaEventRegistration;
import io.quarkus.funqy.runtime.bindings.brasilia.FunqyRequestImpl;
import io.quarkus.funqy.runtime.bindings.brasilia.FunqyResponseImpl;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public final class Worker {

    private static final Logger log = Logger.getLogger(Worker.class);

    private final String appId;
    private final String appVersion;

    private final String workerInstanceId;
    private Duration heartBeatInterval;

    private final Poller poller;

    private final Vertx vertx;
    private final ExecutorService executor;
    private final ObjectMapper mapper;
    private final HttpHostClient httpHostClient;
    private final Map<String, FunctionInvoker> invokersByName;
    private final Collection<BrasiliaEventRegistration> registrations;

    public Worker(String appId,
            String appVersion,
            Vertx vertx,
            ExecutorService executor,
            ObjectMapper mapper,
            Collection<FunctionInvoker> invokers,
            Collection<BrasiliaEventRegistration> registrations) {
        this.appId = appId;
        this.appVersion = appVersion;
        this.vertx = vertx;
        this.mapper = mapper;
        this.executor = executor;
        this.httpHostClient = new HttpHostClient(vertx, mapper);
        Map<String, FunctionInvoker> invokersByName = new HashMap<String, FunctionInvoker>();
        for (FunctionInvoker invoker : invokers) {
            invokersByName.put(invoker.getName(), invoker);
        }
        this.invokersByName = Collections.unmodifiableMap(invokersByName);
        this.registrations = Collections.unmodifiableList(new ArrayList<>(registrations));
        this.workerInstanceId = UUID.randomUUID().toString();

        this.poller = new Poller(workerInstanceId, vertx, httpHostClient, executor, this::handleEvent);
    }

    private Uni<Message.CompleteEvent.InvocationResult> handleEvent(Message.Event event) {
        String[] parts = event.name.split(":");
        String functionName = parts[2];
        FunctionInvoker invoker = invokersByName.get(functionName);

        // TODO set input and context
        FunqyRequestImpl funqyRequest = new FunqyRequestImpl(null, null);
        FunqyResponseImpl funqyResponse = new FunqyResponseImpl();

        invoker.invoke(funqyRequest, funqyResponse);
        return funqyResponse
                .getOutput()
                .onItem()
                .apply(out -> new Message.CompleteEvent.InvocationResult(out, SUCCEEDED, null));

    }

    public Uni<Void> start() {
        log.infof("Starting Brasilia Worker...");
        return httpHostClient
                .connect(workerInstanceId, appId, appVersion, Arrays.asList(new String[] { "http" }))
                .onItem()
                .produceUni(connectResult -> {
                    log.debugf("Channel configurations: %s.", connectResult.channelConfigurations.toString());
                    if (!connectResult.channelConfigurations.containsKey("http")) {
                        throw new RuntimeException("Host doesn't support http.");
                    }
                    long interval = connectResult.workerHearbeatTimeoutInSeconds * 1000 - 1000;
                    heartBeatInterval = Duration.ofMillis(interval > 0 ? interval : 1000);
                    return httpHostClient.registerEvents(workerInstanceId, getRegistrations());
                })
                .onItem()
                .apply(eventRegistrationsResult -> {
                    log.debugf("Event registration result: %s.", eventRegistrationsResult.toString());
                    RuntimeException ex = EventRegistrationsResultToError(eventRegistrationsResult);
                    if (ex != null) {
                        throw ex;
                    }
                    startHeartBeat();
                    poller.start();
                    return null;
                });
    }

    private List<Message.EventRegistrations.EventRegistration> getRegistrations() {
        return this.registrations
                .stream()
                .map(r -> {
                    try {
                        return new Message.EventRegistrations.EventRegistration(
                                r.eventProviderId,
                                r.eventProviderVersion,
                                r.functionName,
                                mapper.readValue(r.configJSON, Map.class));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private RuntimeException EventRegistrationsResultToError(Message.EventRegistrationsResult eventRegistrationsResult) {
        if (!eventRegistrationsResult.success) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error on registration: ");
            sb.append(eventRegistrationsResult.failureMessage);
            sb.append(".");
            sb.append("Errors: ");
            sb.append(String.join(";",
                    eventRegistrationsResult.eventRegistrationResults
                            .stream()
                            .filter(o -> !o.success)
                            .map(o -> o.failureMessage).collect(Collectors.toList())));
            sb.append(".");
            return new RuntimeException(sb.toString());
        }
        return null;
    }

    private long timerId = -1;

    private void startHeartBeat() {
        timerId = vertx.setPeriodic(this.heartBeatInterval.toMillis(), timer -> {
            httpHostClient
                    .heartBeat(workerInstanceId)
                    .subscribe()
                    .with(_void -> log.debugf("HeartBeat has been sent."), t -> log.error("Error on heartbeat", t));
        });
    }

    private void stopHeartBeat() {
        vertx.cancelTimer(timerId);
    }

    public Uni<Void> stop() {
        log.infof("Stopping Brasilia Worker...");
        return poller.stop()
                .onItem()
                .produceUni((_void) -> httpHostClient.unregister(workerInstanceId))
                .onItem()
                .produceUni((_void) -> httpHostClient.disconnect(workerInstanceId))
                .onItem()
                .invoke((_void) -> stopHeartBeat());
    }
}
