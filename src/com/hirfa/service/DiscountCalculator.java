package com.hirfa.service;

import com.hirfa.model.Outcome;

public class DiscountCalculator {

    public Outcome apply(String code, double subtotal) {
        if (code == null || code.isBlank()) return Outcome.ok("No promotion code.", 0);
        switch (code.trim().toUpperCase()) {
            case "HIRFA10" -> {
                if (subtotal < 10) return Outcome.fail("HIRFA10 applies to orders of 10 JOD or more.");
                return Outcome.ok("HIRFA10 applied: 10% discount.", round(subtotal * 0.10));
            }
            case "STUDENTS5" -> {
                return Outcome.ok("STUDENTS5 applied: 5% discount.", round(subtotal * 0.05));
            }
            default -> {
                return Outcome.fail("Promotion code " + code.trim() + " is not supported.");
            }
        }
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
