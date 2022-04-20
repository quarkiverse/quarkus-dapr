package io.quarkiverse.dapr.endpoint.actor;

import io.dapr.actors.runtime.ActorRuntime;
import io.vertx.ext.web.RoutingContext;

/**
 * ActorInvokeTimerHandler
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class ActorInvokeTimerHandler extends AbstractDaprActorHandler {
    @Override
    public String subRoute() {
        return ":type/:id/method/timer/:timer";
    }

    /**
     * Handles API to trigger an actor's timer.
     *
     * @param type Actor type.
     * @param id Actor Id.
     * @param timer Actor timer's name.
     * @param body Raw request's body.
     * @return Void.
     */
    @Override
    protected void put(RoutingContext event) {
        String type = event.pathParam("type");
        String id = event.pathParam("id");
        String timer = event.pathParam("timer");
        byte[] body = event.getBody().getBytes();
        event.json(ActorRuntime.getInstance().invokeTimer(type, id, timer, body).block());
    }
}
