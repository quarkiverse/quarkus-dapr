package io.quarkiverse.dapr.endpoint.actor;

import io.quarkiverse.dapr.endpoint.dapr.AbstractDaprHandler;

/**
 * AbstractDaprActorHandler
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public abstract class AbstractDaprActorHandler extends AbstractDaprHandler {

    @Override
    public String baseRoute() {
        return "/actors";
    }

}
