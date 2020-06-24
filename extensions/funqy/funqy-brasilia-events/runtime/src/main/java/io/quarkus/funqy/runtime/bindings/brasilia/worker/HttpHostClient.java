package io.quarkus.funqy.runtime.bindings.brasilia.worker;

import static io.quarkus.funqy.runtime.bindings.brasilia.worker.Message.classToCloudEventType;
import static io.quarkus.funqy.runtime.bindings.brasilia.worker.Message.cloudEventTypeToClass;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.jackson.JsonFormat;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public final class HttpHostClient {

    private static final URI workerSource = URI.create("/ofQuarkusWorker");
    private static final String dataContentType = "application/json; charset=utf-8";
    private static final String contentType = "application/cloudevents+json; charset=utf-8";

    private final WebClient webClient;
    private final EventFormat format = new JsonFormat();
    private final ObjectMapper mapper;

    public HttpHostClient(Vertx vertx, ObjectMapper mapper) {

        WebClientOptions httpOptions = new WebClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(5001);

        webClient = WebClient.create(io.vertx.mutiny.core.Vertx.newInstance(vertx), httpOptions);

        this.mapper = mapper;

    }

    private static CloudEventBuilder createBuilder() {
        return CloudEventBuilder
                .v1()
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDataContentType(dataContentType)
                .withSource(workerSource);
    }

    private CloudEvent dataToCloudEvent(Object obj) {
        try {
            if (obj == null)
                return null;
            if (!classToCloudEventType.containsKey(obj.getClass())) {
                throw new RuntimeException("Parameter is not recognized message.");
            }
            // host does double marshaling
            String s = mapper.writeValueAsString(obj);
            byte[] data = mapper.writeValueAsBytes(s);
            return createBuilder()
                    .withType(classToCloudEventType.get(obj.getClass()))
                    .withData(data)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Object cloudEventToData(CloudEvent cloudEvent) {
        try {
            if (cloudEvent == null)
                return null;
            if (!cloudEventTypeToClass.containsKey(cloudEvent.getType())) {
                throw new RuntimeException("Unknown message type: " + cloudEvent.getType());
            }
            // host does double marshaling
            String s = mapper.readValue(cloudEvent.getData(), String.class);
            return mapper.readValue(s, cloudEventTypeToClass.get(cloudEvent.getType()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Uni<CloudEvent> postCloudEvent(CloudEvent cloudEvent) {
        HttpRequest<io.vertx.mutiny.core.buffer.Buffer> request = webClient.post("/");
        request.putHeader("Content-Type", contentType);
        return request.sendBuffer(Buffer.buffer(format.serialize(cloudEvent)))
                .onItem()
                .apply(bufferHttpResponse -> {
                    if (bufferHttpResponse.body() != null) {
                        return format.deserialize(bufferHttpResponse.body().getBytes());
                    } else {
                        return null;
                    }
                });
    }

    private <Resp, Req> Uni<Resp> postData(Req object) {
        CloudEvent cloudEvent = dataToCloudEvent(object);
        return postCloudEvent(cloudEvent)
                .onItem()
                .apply(ce -> (Resp) cloudEventToData(ce));
    }

    public Uni<Message.ConnectResult> connect(String workerInstanceId, String applicationId, String applicationVersion,
            List<String> capabilities) {
        return postData(new Message.Connect(applicationId, applicationVersion, workerInstanceId, capabilities));
    }

    public Uni<Message.EventRegistrationsResult> registerEvents(String workerInstanceId,
            List<Message.EventRegistrations.EventRegistration> eventRegistrations) {
        return postData(new Message.EventRegistrations(workerInstanceId, eventRegistrations));
    }

    public Uni<Message.Event[]> getEvents(String workerInstanceId, long count) {
        return postData(new Message.GetEvents(workerInstanceId, count));
    }

    public Uni<Void> heartBeat(String workerInstanceId) {
        return postData(new Message.HeartBeat(workerInstanceId));
    }

    public Uni<Void> completeEvent(String eventId, Object data, Message.CompleteEvent.InvocationResult.InvocationStatus status,
            String timeoutReason) {
        return postData(
                new Message.CompleteEvent(eventId, new Message.CompleteEvent.InvocationResult(data, status, timeoutReason)));
    }

    public Uni<Void> unregister(String workerInstanceId) {
        return postData(new Message.Unregister(workerInstanceId));
    }

    public Uni<Void> disconnect(String workerInstanceId) {
        return postData(new Message.Disconnect(workerInstanceId));
    }
}
