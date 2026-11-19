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
        if (p == null) return Outcome.fail("Product not found.");
        if (qty < 1) return Outcome.fail("Quantity must be positive.");
        int updatedQty = cart.qtyOf(productId) + qty;
        if (updatedQty > MAX_QTY_PER_PRODUCT) return Outcome.fail("You can add up to 5 pieces per product.");
        if (updatedQty > p.stock) return Outcome.fail("Requested quantity is out of stock.");
        cart.set(productId, updatedQty);
        return Outcome.ok("Added " + qty + " of " + p.name + " to your cart.");
    }

    public Outcome changeQty(Cart cart, int productId, int newQty) {
        Product p = repo.product(productId);
        if (p == null) return Outcome.fail("Product not found.");
        if (newQty < 1 || newQty > MAX_QTY_PER_PRODUCT) return Outcome.fail("Quantity must be from 1 to 5.");
        if (newQty > p.stock) return Outcome.fail("Requested quantity is out of stock.");
        cart.set(productId, newQty);
        return Outcome.ok("Cart quantity for " + p.name + " is now " + newQty + ".");
    }

    public Outcome removeItem(Cart cart, int productId) {
        if (repo.product(productId) == null) return Outcome.fail("Product not found.");
        if (cart.qtyOf(productId) == 0) return Outcome.fail("Product is not in your cart.");
        cart.remove(productId);
        return Outcome.ok("Item removed from your cart.");
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
