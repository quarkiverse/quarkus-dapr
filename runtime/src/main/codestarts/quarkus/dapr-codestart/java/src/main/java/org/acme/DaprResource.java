package org.acme;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;
import io.quarkiverse.dapr.core.SyncDaprClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

@Path("/dapr")
public class DaprResource {

    @Inject
    SyncDaprClient client;

    @POST
    @Path("/state")
    public Response saveState() {
        client.saveState("kvstore", "identity", UUID.randomUUID().toString());
        return Response.ok().build();
    }

    @GET
    @Path("/state")
    public Response getState() {
        State<String> state = client.getState("kvstore", "identity", String.class);
        return Response.ok(Map.of("identity", state.getValue())).build();
    }

    @POST
    @Path("/pub")
    public void pub() {
        client.publishEvent("pubsub", "topicName", "Hello from Quarkus!");
    }

    @POST
    @Topic(name = "topicName")
    @Path("/sub")
    public void sub(CloudEvent<String> event) {
        System.out.println("Received event: " + event.getData());
    }
}