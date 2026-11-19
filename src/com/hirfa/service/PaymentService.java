package com.hirfa.service;

import com.hirfa.model.Outcome;

public class PaymentService {

    public Outcome pay(String method, double amount, double walletBalance, String cardNumber) {
        if (method == null) return Outcome.fail("Payment method is not supported.");
        if (!Double.isFinite(amount) || amount < 0) return Outcome.fail("Payment amount must not be negative.");
        return switch (method) {
            case "CASH" -> amount <= 500
                    ? Outcome.ok("Cash on delivery accepted.")
                    : Outcome.fail("Cash on delivery supports orders up to 500 JOD.");
            case "CARD" -> cardNumber != null && cardNumber.matches("\\d{16}")
                    ? Outcome.ok("Card payment confirmed.")
                    : Outcome.fail("Card number must be exactly 16 digits.");
            case "WALLET" -> walletBalance >= amount
                        ? Outcome.ok("Wallet payment accepted.")
                        : Outcome.fail("Insufficient wallet balance.");
            default -> Outcome.fail("Payment method is not supported.");
        };
    }
}
