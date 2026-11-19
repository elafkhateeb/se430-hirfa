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
        return place(customer, cart, distanceKm, payment, promoCode, null, now);
    }

    public Outcome place(Customer customer, Cart cart, double distanceKm, String payment,
                         String promoCode, String cardNumber, LocalDateTime now) {
        if (cart.isEmpty()) return Outcome.fail("Your cart is empty.");
        if (!Double.isFinite(distanceKm) || distanceKm < 0) return Outcome.fail("Delivery distance must be zero or greater.");
        List<OrderItem> items = buildItems(cart);
        if (items == null) return Outcome.fail("A cart item is no longer available.");
        double subtotal = items.stream().mapToDouble(OrderItem::subtotal).sum();
        if (subtotal < MIN_ORDER_TOTAL) return Outcome.fail("Orders must total at least 5 JOD.");

        DiscountCalculator discountCalc = new DiscountCalculator();
        Outcome promo = discountCalc.apply(promoCode, subtotal);
        if (!promo.ok()) return Outcome.fail(promo.message());

        DeliveryFeeCalculator feeCalc = new DeliveryFeeCalculator();
        double fee = feeCalc.fee(subtotal, distanceKm);
        Tier tier = new LoyaltyService().tierFor(customer.ordersCount);
        double discount = round(promo.value() + subtotal * tier.discountPercent() / 100.0);
        double total = round(subtotal - discount + fee);

        Outcome paymentResult = new PaymentService().pay(payment, total, customer.wallet, cardNumber);
        if (!paymentResult.ok()) return paymentResult;

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
        return new Outcome(true, "Order #" + orderId + " created — total " + total + " JOD.", orderId);
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

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
