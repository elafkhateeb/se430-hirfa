package com.hirfa.service;

import com.hirfa.model.Order;
import com.hirfa.model.Outcome;
import java.time.LocalDateTime;

public class OrderLifecycleService {
    public static final int CANCEL_WINDOW_MINUTES = 120;

    public Outcome cancel(Order order, LocalDateTime now) {
        if (order.status != Order.Status.NEW) return Outcome.fail("This order can no longer be cancelled.");
        long minutes = java.time.Duration.between(order.createdAt, now).toMinutes();
        if (minutes > CANCEL_WINDOW_MINUTES) return Outcome.fail("The two-hour cancellation window has expired.");
        order.status = Order.Status.CANCELLED;
        if (order.payment.equals("WALLET")) {
            return Outcome.ok("Order cancelled; " + order.total + " JOD refunded to the wallet.", order.total);
        }
        return Outcome.ok("Order cancelled.", order.total);
    }

    public Outcome advance(Order order) {
        switch (order.status) {
            case NEW -> {
                order.status = Order.Status.PREPARING;
                return Outcome.ok("Order status: Preparing.");
            }
            case PREPARING -> {
                order.status = Order.Status.SHIPPING;
                return Outcome.ok("Order status: Out for delivery.");
            }
            case SHIPPING -> {
                order.status = Order.Status.DELIVERED;
                return Outcome.ok("Order status: Delivered.");
            }
            default -> {
                return Outcome.fail("There is no next order status.");
            }
        }
    }
}
