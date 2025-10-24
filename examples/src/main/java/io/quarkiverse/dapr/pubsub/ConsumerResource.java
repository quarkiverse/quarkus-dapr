package io.quarkiverse.dapr.pubsub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.ResponseStatus;

import io.dapr.Topic;
import io.quarkus.logging.Log;

@Path("/webhooks")
public class ConsumerResource {

    public static final List<OrderEvent> ORDERS = Collections.synchronizedList(new ArrayList<>());

    @POST
    @ResponseStatus(200)
    @Topic(pubsubName = "pubsub", name = "orders.created")
    public void handleOrderCreated(OrderEvent order) {
        System.out.println("Received order created event: " + order.getId());
        ORDERS.add(order);
    }

    @GET
    public List<OrderEvent> orders() {
        Log.info("Returning orders: " + ORDERS.size());
        return ORDERS;
    }
}
