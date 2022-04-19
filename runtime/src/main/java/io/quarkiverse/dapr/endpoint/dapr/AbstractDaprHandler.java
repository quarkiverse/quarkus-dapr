package io.quarkiverse.dapr.endpoint.dapr;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * AbstractDaprHandler
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public abstract class AbstractDaprHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        HttpMethod method = event.request().method();
        if (HttpMethod.GET.equals(method)) {
            get(event);
        } else if (HttpMethod.POST.equals(method)) {
            post(event);
        } else if (HttpMethod.PUT.equals(method)) {
            put(event);
        } else if (HttpMethod.DELETE.equals(method)) {
            delete(event);
        } else {
            other(event);
        }

    }

    public String baseRoute() {
        return "/dapr";
    }

    abstract public String subRoute();

    protected void get(RoutingContext event) {

    }

    protected void post(RoutingContext event) {

    }

    protected void put(RoutingContext event) {

    }

    protected void delete(RoutingContext event) {

    }

    protected void other(RoutingContext event) {

    }
}
