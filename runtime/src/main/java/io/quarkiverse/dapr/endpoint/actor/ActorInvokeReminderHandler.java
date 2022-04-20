package io.quarkiverse.dapr.endpoint.actor;

import io.dapr.actors.runtime.ActorRuntime;
import io.vertx.ext.web.RoutingContext;

/**
 * ActorInvokeReminderHandler
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class ActorInvokeReminderHandler extends AbstractDaprActorHandler {
    @Override
    public String subRoute() {
        return ":type/:id/method/remind/:reminder";
    }

    /**
     * Handles API to trigger an actor's reminder.
     *
     * @param type Actor type.
     * @param id Actor Id.
     * @param reminder Actor reminder's name.
     * @param body Raw request's body.
     * @return Void.
     */
    @Override
    protected void put(RoutingContext event) {
        String type = event.pathParam("type");
        String id = event.pathParam("id");
        String reminder = event.pathParam("reminder");
        byte[] body = event.getBody().getBytes();
        ActorRuntime.getInstance().invokeReminder(type, id, reminder, body).block();
    }
}
