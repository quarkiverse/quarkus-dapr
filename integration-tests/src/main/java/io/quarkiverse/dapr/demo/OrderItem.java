package io.quarkiverse.dapr.demo;

public class OrderItem {
    public final Long id;
    public final String name;
    public final double price;

    public OrderItem(Long id, String name, double price) {
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
