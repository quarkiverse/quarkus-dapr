package io.quarkiverse.dapr.pubsub;

import java.math.BigDecimal;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class OrderEvent {

    private final String id;
    private final List<OrderItem> items;

    public OrderEvent(String id, List<OrderItem> items) {
        this.id = id;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    @RegisterForReflection
    public static class OrderItem {

        public OrderItem(String id, String name, Integer quantity, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        private String id;
        private String name;
        private Integer quantity;
        private BigDecimal price;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }
}
