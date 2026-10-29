package com.hirfa.model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    public enum Status { NEW, PREPARING, SHIPPING, DELIVERED, CANCELLED }

    public int id;
    public final int customerId;
    public final List<OrderItem> items;
    public final String payment;
    public final double distanceKm;
    public final LocalDateTime createdAt;
    public double subtotal;
    public double fee;
    public double discount;
    public double total;
    public Status status;

    public Order(int customerId, List<OrderItem> items, String payment, double distanceKm, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.items = items;
        this.payment = payment;
        this.distanceKm = distanceKm;
        this.createdAt = createdAt;
        this.subtotal = items.stream().mapToDouble(OrderItem::subtotal).sum();
        this.status = Status.NEW;
    }

    public Order(int id, int customerId, List<OrderItem> items, String payment, double distanceKm,
                 LocalDateTime createdAt, double subtotal, double fee, double discount, double total, Status status) {
        this(customerId, items, payment, distanceKm, createdAt);
        this.id = id;
        this.subtotal = subtotal;
        this.fee = fee;
        this.discount = discount;
        this.total = total;
        this.status = status;
    }
}
