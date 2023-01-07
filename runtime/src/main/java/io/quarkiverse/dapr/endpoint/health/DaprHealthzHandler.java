package io.quarkiverse.dapr.endpoint.health;

import java.io.IOException;

import io.quarkiverse.dapr.endpoint.dapr.AbstractDaprHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * DaprHealthzHandler
 *
 * @author nayan
 * @date 2022/11/17 21:22
 */
public class DaprHealthzHandler extends AbstractDaprHandler {
    @Override
    public String baseRoute() {
        return "/healthz";
    }

    @Override
    public String subRoute() {
        return "";
    }

    /**
     * Returns Dapr's configuration for Actors.
     *
     * @return Actor's configuration.
     * @throws IOException If cannot generate configuration.
     */
    @Override
    protected void get(RoutingContext event) {
        event.end();
    }
}
