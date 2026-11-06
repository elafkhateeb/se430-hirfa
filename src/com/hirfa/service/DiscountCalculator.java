package com.hirfa.service;

import com.hirfa.model.Outcome;

public class DiscountCalculator {

    public Outcome apply(String code, double subtotal) {
        if (code == null || code.isBlank()) return Outcome.ok("بدون كود خصم", 0);
        switch (code.trim()) {
            case "حرفة10" -> {
                if (subtotal < 10) return Outcome.fail("الكود حرفة10 متاح للطلبات من 10 دنانير");
                return Outcome.ok("تم تطبيق خصم 10%", round(subtotal * 0.10));
            }
            case "طلاب5" -> {
                return Outcome.ok("تم تطبيق خصم 5%", round(subtotal * 0.05));
            }
            default -> {
                return Outcome.fail("الكود " + code.trim() + " غير معتمد");
            }
        }
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
