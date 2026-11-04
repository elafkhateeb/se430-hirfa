package com.hirfa.service;

import com.hirfa.model.*;
import com.hirfa.repo.Repo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderService {
    public static final double MIN_ORDER_TOTAL = 5;
    private final Repo repo;

    public OrderService(Repo repo) {
        this.repo = repo;
    }

    public Outcome place(Customer customer, Cart cart, double distanceKm, String payment, String promoCode, LocalDateTime now) {
        if (cart.isEmpty()) return Outcome.fail("السلة فارغة");
        List<OrderItem> items = buildItems(cart);
        if (items == null) return Outcome.fail("أحد المنتجات في السلة لم يعد متوفراً");
        double subtotal = items.stream().mapToDouble(OrderItem::subtotal).sum();
        if (subtotal < MIN_ORDER_TOTAL) return Outcome.fail("الحد الأدنى لقيمة الطلب 5 دنانير");
        if (!validMethod(payment)) return Outcome.fail("طريقة الدفع غير معتمدة");

        DiscountCalculator discountCalc = new DiscountCalculator();
        Outcome promo = discountCalc.apply(promoCode, subtotal);
        if (!promo.ok()) return Outcome.fail(promo.message());

        DeliveryFeeCalculator feeCalc = new DeliveryFeeCalculator();
        double fee = feeCalc.fee(subtotal, distanceKm);
        Tier tier = new LoyaltyService().tierFor(customer.ordersCount);
        double discount = round(promo.value() + subtotal * tier.discountPercent() / 100.0);
        double total = round(subtotal - discount + fee);

        if (payment.equals("WALLET") && customer.wallet < total) {
            return Outcome.fail("الرصيد في المحفظة غير كافٍ");
        }

        for (OrderItem it : items) {
            Product p = repo.product(it.productId());
            p.stock -= it.qty();
            repo.saveProduct(p);
        }
        Order order = new Order(customer.id, items, payment, distanceKm, now);
        order.fee = fee;
        order.discount = discount;
        order.total = total;
        int orderId = repo.createOrder(order);
        customer.ordersCount++;
        if (payment.equals("WALLET")) customer.wallet = round(customer.wallet - total);
        repo.saveCustomer(customer);
        return new Outcome(true, "تم إنشاء الطلب رقم " + orderId + " بقيمة " + total + " دينار", orderId);
    }

    private List<OrderItem> buildItems(Cart cart) {
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cart.all().entrySet()) {
            Product p = repo.product(e.getKey());
            if (p == null || p.stock < e.getValue()) return null;
            items.add(new OrderItem(p.id, p.name, p.price, e.getValue()));
        }
        return items;
    }

    private boolean validMethod(String method) {
        return method != null && (method.equals("CASH") || method.equals("CARD") || method.equals("WALLET"));
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
