package com.hirfa.service;

import com.hirfa.model.Order;
import com.hirfa.model.Outcome;
import java.time.LocalDateTime;

public class OrderLifecycleService {
    public static final int CANCEL_WINDOW_MINUTES = 120;

    public Outcome cancel(Order order, LocalDateTime now) {
        if (order.status != Order.Status.NEW) return Outcome.fail("لا يمكن إلغاء الطلب في هذه المرحلة");
        long minutes = java.time.Duration.between(order.createdAt, now).toMinutes();
        if (minutes > CANCEL_WINDOW_MINUTES) return Outcome.fail("انتهت مدة الإلغاء (ساعتان)");
        order.status = Order.Status.CANCELLED;
        if (order.payment.equals("WALLET")) {
            return Outcome.ok("تم إلغاء الطلب واسترداد " + order.total + " دينار إلى المحفظة", order.total);
        }
        return Outcome.ok("تم إلغاء الطلب", order.total);
    }

    public Outcome advance(Order order) {
        switch (order.status) {
            case NEW -> {
                order.status = Order.Status.PREPARING;
                return Outcome.ok("أصبحت حالة الطلب: قيد التحضير");
            }
            case PREPARING -> {
                order.status = Order.Status.SHIPPING;
                return Outcome.ok("أصبحت حالة الطلب: قيد التوصيل");
            }
            case SHIPPING -> {
                order.status = Order.Status.DELIVERED;
                return Outcome.ok("أصبحت حالة الطلب: تم التسليم");
            }
            default -> {
                return Outcome.fail("لا توجد حالة تالية");
            }
        }
    }
}
