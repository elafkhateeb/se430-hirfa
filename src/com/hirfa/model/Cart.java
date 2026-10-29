package com.hirfa.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cart {
    private final Map<Integer, Integer> quantities = new LinkedHashMap<>();

    public void put(int productId, int qty) {
        quantities.merge(productId, qty, Integer::sum);
    }

    public void set(int productId, int qty) {
        quantities.put(productId, qty);
    }

    public int qtyOf(int productId) {
        return quantities.getOrDefault(productId, 0);
    }

    public Map<Integer, Integer> all() {
        return new LinkedHashMap<>(quantities);
    }

    public boolean isEmpty() {
        return quantities.isEmpty();
    }

    public void clear() {
        quantities.clear();
    }
}
