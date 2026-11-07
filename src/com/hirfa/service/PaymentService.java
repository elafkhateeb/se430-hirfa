package com.hirfa.service;

import com.hirfa.model.Outcome;

public class PaymentService {

    public Outcome pay(String method, double amount, double walletBalance, String cardNumber) {
        if (method == null) return Outcome.fail("طريقة الدفع غير معتمدة");
        return switch (method) {
            case "CASH" -> amount <= 500
                    ? Outcome.ok("الدفع نقداً عند التسليم")
                    : Outcome.fail("قيمة الطلب تتجاوز حد الدفع النقدي (500 دينار)");
            case "CARD" -> cardNumber != null && cardNumber.matches("\\d{16}")
                    ? Outcome.ok("تم تأكيد الدفع بالبطاقة")
                    : Outcome.fail("رقم البطاقة غير صحيح (16 رقماً)");
            case "WALLET" -> walletBalance >= amount
                        ? Outcome.ok("تم الدفع من المحفظة")
                        : Outcome.fail("الرصيد في المحفظة غير كافٍ");
            default -> Outcome.fail("طريقة الدفع غير معتمدة");
        };
    }
}
