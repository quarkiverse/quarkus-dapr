package io.quarkus.reactive.messaging.dapr;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Associates messages with the Vert.x (duplicated) context they were created on, so that every
 * asynchronous callback triggered for a message (ack, nack, client calls) is dispatched back on
 * that same context instead of whatever thread happens to complete the underlying future
 * (e.g. an OkHttp/gRPC I/O thread used by the Dapr client).
 */
final class DaprVertxContext {

    private DaprVertxContext() {
    }

    static Context capture() {
        return VertxContext.createNewDuplicatedContext();
    }

    static void runOnContext(Context context, Runnable action) {
        if (context == null || Vertx.currentContext() == context) {
            action.run();
        } else {
            context.runOnContext(ignored -> action.run());
        }
    }

    static <T> CompletionStage<T> callOnContext(Context context, Supplier<CompletionStage<T>> action) {
        if (context == null || Vertx.currentContext() == context) {
            return action.get();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        context.runOnContext(ignored -> action.get().whenComplete((result, failure) -> {
            if (failure != null) {
                future.completeExceptionally(failure);
            } else {
                future.complete(result);
            }
        }));
        return future;
    }
}
