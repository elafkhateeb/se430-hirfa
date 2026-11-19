package com.hirfa;

import com.hirfa.model.Order;
import com.hirfa.model.OrderItem;
import com.hirfa.model.Outcome;
import com.hirfa.service.OrderLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderLifecycleServiceTest {
    private final OrderLifecycleService service = new OrderLifecycleService();
    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(1, List.of(new OrderItem(3, "Laurel Oil Soap", 3.5, 2)), "CASH", 7,
                LocalDateTime.of(2026, 11, 8, 12, 0));
        order.fee = 2;
        order.total = 9.0;
    }

    @Test
    void cancel_withinWindow_cancelled() {
        LocalDateTime now = order.createdAt.plusMinutes(119);
        Outcome out = service.cancel(order, now);
        assertTrue(out.ok());
        assertTrue(out.message().contains("Order cancelled"));
    }

    @Test
    void cancel_atExactTwoHours_stillAllowed() {
        LocalDateTime now = order.createdAt.plusMinutes(120);
        assertTrue(service.cancel(order, now).ok());
    }

    @Test
    void cancel_afterTwoHours_rejected() {
        LocalDateTime now = order.createdAt.plusMinutes(121);
        Outcome out = service.cancel(order, now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("cancellation window"));
    }

    @Test
    void cancel_deliveredOrder_rejected() {
        order.status = Order.Status.DELIVERED;
        Outcome out = service.cancel(order, order.createdAt.plusMinutes(10));
        assertFalse(out.ok());
        assertTrue(out.message().contains("can no longer be cancelled"));
    }

    @Test
    void cancel_walletPayment_refundMessage() {
        order = new Order(1, order.items, "WALLET", order.distanceKm, order.createdAt);
        Outcome out = service.cancel(order, order.createdAt.plusMinutes(30));
        assertTrue(out.ok());
        assertTrue(out.message().contains("refunded"));
    }

    @Test
    void advance_fullLifecycle() {
        assertTrue(service.advance(order).ok());
        assertTrue(order.status == Order.Status.PREPARING);
        assertTrue(service.advance(order).ok());
        assertTrue(order.status == Order.Status.SHIPPING);
        Outcome out = service.advance(order);
        assertTrue(out.ok());
        assertTrue(order.status == Order.Status.DELIVERED);
        assertTrue(out.message().contains("Delivered"));
    }

    @Test
    void advance_delivered_rejected() {
        order.status = Order.Status.DELIVERED;
        Outcome out = service.advance(order);
        assertFalse(out.ok());
        assertTrue(out.message().contains("no next order status"));
    }
}
