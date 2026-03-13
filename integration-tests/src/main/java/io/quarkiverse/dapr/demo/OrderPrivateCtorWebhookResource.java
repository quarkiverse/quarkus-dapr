package io.quarkiverse.dapr.demo;

import java.util.List;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.dapr.Topic;

@Path("/webhook/orders-private")
public class OrderPrivateCtorWebhookResource {

    public static class Order {
        public final String id;
        public final List<OrderItem> items;

        private Order(String id, List<OrderItem> items) {
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

    public static class OrderItem {
        public final Long id;
        public final String name;
        public final double price;

        private OrderItem(Long id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public Long id() {
            return id;
        }

        public String name() {
            return name;
        }

        public double price() {
            return price;
        }
    }

    @POST
    @Topic(pubsubName = "rabbitmq", name = "order.created")
    public Response consume(Order order) {
        return Response.ok(order.items().size()).build();
    }
}
