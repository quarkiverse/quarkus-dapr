package io.quarkus.reactive.messaging.dapr;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

public class DaprMessage<T> implements Message<T> {

    private final T payload;
    private final Consumer<Throwable> failureHandler;
    private final Runnable successHandler;

    DaprMessage(T payload, Runnable successHandler, Consumer<Throwable> failureHandler) {
        this.payload = payload;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public T getPayload() {
        return payload;
    }

    @Override
    public Supplier<CompletionStage<Void>> getAck() {
        return () -> {
            successHandler.run();
            return CompletableFuture.completedFuture(null);
        };
    }

    @Override
    public Function<Throwable, CompletionStage<Void>> getNack() {
        return error -> {
            failureHandler.accept(error);
            return CompletableFuture.completedFuture(null);
        };
    }

}
