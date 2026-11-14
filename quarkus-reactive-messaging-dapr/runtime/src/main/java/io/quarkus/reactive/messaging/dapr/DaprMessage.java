package io.quarkus.reactive.messaging.dapr;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.Context;

public class DaprMessage<T> implements Message<T> {

    private final T payload;
    private final Consumer<Throwable> failureHandler;
    private final Runnable successHandler;
    private final Context context;

    DaprMessage(T payload, Runnable successHandler, Consumer<Throwable> failureHandler) {
        this.payload = payload;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        // Captured on the event-loop context of the incoming HTTP request, so ack/nack always
        // dispatch back to a valid Vert.x context, whichever thread ultimately triggers them.
        this.context = DaprVertxContext.capture();
    }

    @Override
    public T getPayload() {
        return payload;
    }

    @Override
    public Supplier<CompletionStage<Void>> getAck() {
        return () -> runOnMessageContext(successHandler);
    }

    @Override
    public Function<Throwable, CompletionStage<Void>> getNack() {
        return error -> runOnMessageContext(() -> failureHandler.accept(error));
    }

    private CompletionStage<Void> runOnMessageContext(Runnable action) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DaprVertxContext.runOnContext(context, () -> {
            try {
                action.run();
                future.complete(null);
            } catch (RuntimeException any) {
                future.completeExceptionally(any);
            }
        });
        return future;
    }

}
