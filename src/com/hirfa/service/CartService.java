package com.hirfa.service;

import com.hirfa.model.Cart;
import com.hirfa.model.Outcome;
import com.hirfa.model.Product;
import com.hirfa.repo.Repo;

public class CartService {
    public static final int MAX_QTY_PER_PRODUCT = 5;
    private final Repo repo;

    public CartService(Repo repo) {
        this.repo = repo;
    }

    public Outcome addItem(Cart cart, int productId, int qty) {
        Product p = repo.product(productId);
        if (p == null) return Outcome.fail("المنتج غير موجود");
        if (qty < 1) return Outcome.fail("الكمية يجب أن تكون رقماً موجباً");
        if (qty > MAX_QTY_PER_PRODUCT) return Outcome.fail("الحد الأقصى 5 قطع من كل منتج في السلة");
        if (qty > p.stock) return Outcome.fail("الكمية المطلوبة غير متوفرة حالياً");
        cart.put(productId, qty);
        return Outcome.ok("تمت إضافة " + p.name + " x" + qty + " إلى السلة");
    }

    public Outcome changeQty(Cart cart, int productId, int newQty) {
        Product p = repo.product(productId);
        if (p == null) return Outcome.fail("المنتج غير موجود");
        if (newQty < 1 || newQty > MAX_QTY_PER_PRODUCT) return Outcome.fail("الكمية المسموحة من 1 إلى 5");
        if (newQty > p.stock) return Outcome.fail("الكمية المطلوبة غير متوفرة حالياً");
        cart.set(productId, newQty);
        return Outcome.ok("صارت كمية " + p.name + " في السلة " + newQty);
    }

    public double total(Cart cart) {
        double t = 0;
        for (var entry : cart.all().entrySet()) {
            Product p = repo.product(entry.getKey());
            t += p.price * entry.getValue();
        }
        return Math.round(t * 100.0) / 100.0;
    }
}
