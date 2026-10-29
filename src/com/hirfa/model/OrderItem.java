package com.hirfa.model;

public record OrderItem(int productId, String name, double unitPrice, int qty) {
    public double subtotal() {
        return unitPrice * qty;
    }
}
