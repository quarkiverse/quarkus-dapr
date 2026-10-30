package io.quarkus.reactive.messaging.dapr;

import java.util.function.Consumer;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public class RouteFunction implements Consumer<Route> {

    private String path;

    private Handler<RoutingContext> handler;

    public RouteFunction() {
        // Required for serialization
    }

    public RouteFunction(String path, Handler<RoutingContext> handler) {
        this.path = path;
        this.handler = handler;
    }

    public Handler<RoutingContext> getHandler() {
        // Required for serialization
        return handler;
    }

    public String getPath() {
        // Required for serialization
        return path;
    }

    public void setHandler(Handler<RoutingContext> handler) {
        // Required for serialization
        this.handler = handler;
    }

    public void setPath(String path) {
        // Required for serialization
        this.path = path;
    }

    @Override
    public void accept(Route route) {
        if (handler != null) {
            route.method(HttpMethod.POST).path(path).handler(handler);
            route.method(HttpMethod.PUT).path(path).handler(handler);
        }
    }
}
