package io.quarkus.funqy.runtime.bindings.brasilia.worker;

import static io.quarkus.funqy.runtime.bindings.brasilia.worker.Message.CompleteEvent.InvocationResult.InvocationStatus.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.builders.EmitterBasedMulti;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.Vertx;

public final class Poller {

    private static final Logger log = Logger.getLogger(Poller.class);

    private final String workerInstanceId;
    private final Vertx vertx;
    private final HttpHostClient httpHostClient;
    private final ExecutorService executor;
    private final EventHandler eventHandler;
    private Cancellable pollingCancellable;

    public Poller(String workerInstanceId, Vertx vertx, HttpHostClient httpHostClient, ExecutorService executor,
            EventHandler eventHandler) {
        this.workerInstanceId = workerInstanceId;
        this.vertx = vertx;
        this.httpHostClient = httpHostClient;
        this.executor = executor;
        this.eventHandler = eventHandler;
    }

    private final CompletableFuture<Void> allInvocationStopped = new CompletableFuture<>();
    private final AtomicReference<InProgressState> state = new AtomicReference<>(new InProgressState(true, 0));

    private static final class InProgressState {
        public final boolean shouldRun;
        public final int inProgress;

        private InProgressState(boolean shouldRun, int inProgress) {
            this.shouldRun = shouldRun;
            this.inProgress = inProgress;
        }
    }

    private boolean incInProgress() {
        InProgressState newSate = this.state.updateAndGet(s -> s.shouldRun ? new InProgressState(true, s.inProgress + 1) : s);
        return newSate.shouldRun;
    }

    private void decInProgress() {
        InProgressState newSate = this.state.updateAndGet(s -> new InProgressState(s.shouldRun, s.inProgress - 1));
        if (!newSate.shouldRun && newSate.inProgress == 0) {
            allInvocationStopped.complete(null);
        }
    }

    private void dispatchEvent(Message.Event event) {
        if (!incInProgress()) {
            httpHostClient
                    .completeEvent(event.id, "Worker is shutting down.", UNHANDLED, null)
                    .subscribe()
                    .with(o -> {
                    }, t2 -> log.error("Error on completeEvent()", t2));
            return;
        }
        try {
            log.debugf("Dispatching event %s.", event);
            eventHandler
                    .handle(event)
                    .subscribe()
                    .with(invocationResult -> {
                        decInProgress();
                        log.debugf("Posting InvocationResult: %s for event with id: %s.", invocationResult, event.id);
                        httpHostClient
                                .completeEvent(event.id, invocationResult.data, invocationResult.status,
                                        invocationResult.timeoutReason)
                                .subscribe()
                                .with(o -> {
                                }, t2 -> log.error("Error on completeEvent()", t2));
                    }, t -> {
                        decInProgress();
                        httpHostClient
                                .completeEvent(event.id, t.getMessage(), FAILED, null)
                                .subscribe()
                                .with(o -> {
                                }, t2 -> log.error("Error on completeEvent()", t2));
                        log.error("Error on dispatchEvent()", t);
                    });
        } catch (Throwable t) {
            decInProgress();
            httpHostClient
                    .completeEvent(event.id, t.getMessage(), FAILED, null)
                    .subscribe()
                    .with(o -> {
                    }, t2 -> log.error("Error on completeEvent()", t2));
            log.error("Error on dispatchEvent()", t);
        }
    }

    private void getEventRec(MultiEmitter<? super Message.Event> emitter) {
        if (emitter.isCancelled()) {
            return;
        }

        long cnt = emitter.requested();

        if (cnt == Long.MAX_VALUE || cnt == 0) {
            vertx.runOnContext(_void -> getEventRec(emitter));
            return;
        }
        httpHostClient.getEvents(workerInstanceId, cnt)
                .subscribe()
                .with(
                        events -> {
                            for (Message.Event event : events) {
                                emitter.emit(event);
                            }
                            vertx.runOnContext(_void -> getEventRec(emitter));
                        },
                        t -> emitter.fail(t));
    }

    Uni<Void> start() {

        Multi<Message.Event> events = new EmitterBasedMulti<>(multiEmitter -> getEventRec(multiEmitter),
                BackPressureStrategy.BUFFER);
        pollingCancellable = events
                .emitOn(executor)
                .subscribe()
                .with(event -> executor.execute(() -> dispatchEvent(event)),
                        t -> log.error("Error on polling.", t));

        return Uni.createFrom().nullItem();
    }

    Uni<Void> stop() {
        pollingCancellable.cancel();
        InProgressState newState = state.updateAndGet(s -> new InProgressState(false, s.inProgress));
        if (newState.inProgress == 0) {
            allInvocationStopped.complete(null);
        }
        return Uni.createFrom()
                .completionStage(allInvocationStopped)
                .ifNoItem()
                .after(Duration.ofMinutes(1))
                .failWith(new RuntimeException("Failed to stop all ongoing function invocations."));
    }

    @FunctionalInterface
    public interface EventHandler {
        Uni<Message.CompleteEvent.InvocationResult> handle(Message.Event event);
    }
}
