package io.quarkiverse.dapr.demo;

import java.util.List;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.dapr.Topic;

@Path("/webhook/orders")
public class OrderWebhookResource {

    public static class Order {
        public String id;
        public List<OrderItem> items;
    }

    public static class OrderItem {
        public Long id;
        public String name;
        public double price;
    }

    @POST
    @Topic(pubsubName = "rabbitmq", name = "order.created")
    public Response consume(Order order) {
        return Response.ok(order.items.size()).build();
    }
}
