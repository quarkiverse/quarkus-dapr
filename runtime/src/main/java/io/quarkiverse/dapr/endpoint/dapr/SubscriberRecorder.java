package io.quarkiverse.dapr.endpoint.dapr;

import java.util.function.Consumer;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SubscriberRecorder {

    public Consumer<Route> route() {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.method(HttpMethod.GET);
            }
        };
    }

    public Handler<RoutingContext> handler(String json) {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                HttpServerResponse serverResponse = event.response();
                serverResponse.headers().add("Content-Type", "application/json");
                serverResponse.setStatusCode(200);
                serverResponse.send(json);
            }
        };
    }
}
