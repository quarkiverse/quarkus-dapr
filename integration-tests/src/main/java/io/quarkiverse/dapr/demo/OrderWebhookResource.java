package io.quarkiverse.dapr.demo;

import java.util.List;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.dapr.Topic;

@Path("/webhook/orders")
public class OrderWebhookResource {

    public static class Order {
        public final String id;
        public final List<OrderItem> items;

        Order(String id, List<OrderItem> items) {
            this.id = id;
            this.items = items;
        }

        public String id() {
            return id;
        }

        public List<OrderItem> items() {
            return items;
        }
    }

    @POST
    @Topic(pubsubName = "rabbitmq", name = "order.created")
    public Response consume(Order order) {
        return Response.ok(order.items().size()).build();
    }
}
