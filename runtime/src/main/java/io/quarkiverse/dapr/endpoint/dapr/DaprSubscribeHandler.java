package io.quarkiverse.dapr.endpoint.dapr;

import io.quarkiverse.dapr.core.DaprRuntime;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

/**
 * DaprSubscribeHandler
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class DaprSubscribeHandler extends AbstractDaprHandler {

    @Override
    public String subRoute() {
        return "subscribe";
    }

    /**
     * Returns the list of subscribed topics.
     *
     * @return List of subscribed topics.
     * @throws IOException If cannot generate list of topics.
     */
    @Override
    protected void get(RoutingContext event) {
        event.json(DaprRuntime.getInstance().listSubscribedTopics());
    }
}
