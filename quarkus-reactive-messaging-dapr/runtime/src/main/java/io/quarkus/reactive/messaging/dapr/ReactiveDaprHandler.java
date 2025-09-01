package io.quarkus.reactive.messaging.dapr;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ReactiveDaprHandler implements Handler<RoutingContext> {
    private final ReactiveDaprHandlerBean handler;

    public ReactiveDaprHandler(ReactiveDaprHandlerBean handler) {
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext event) {
        try {
            handler.handle(event);
        } catch (RuntimeException any) {
            event.fail(any);
        }
    }
}
