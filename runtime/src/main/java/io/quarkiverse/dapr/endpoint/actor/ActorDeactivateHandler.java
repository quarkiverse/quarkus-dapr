package io.quarkiverse.dapr.endpoint.actor;

import io.dapr.actors.runtime.ActorRuntime;
import io.vertx.ext.web.RoutingContext;

/**
 * ActorDeactivateHandler
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class ActorDeactivateHandler extends AbstractDaprActorHandler {
    @Override
    public String subRoute() {
        return ":type/:id";
    }

    /**
     * Handles API to deactivate an actor.
     *
     * @param type Actor type.
     * @param id Actor Id.
     * @return Void.
     */
    @Override
    protected void delete(RoutingContext event) {
        String type = event.pathParam("type");
        String id = event.pathParam("id");
        ActorRuntime.getInstance().deactivate(type, id).block();
    }
}
