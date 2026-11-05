package com.hirfa.service;

import com.hirfa.model.Outcome;

public class DeliveryFeeCalculator {

    public double fee(double subtotal, double distanceKm) {
        if (distanceKm < 0) return -1;
        if (subtotal >= 100) return 0;
        if (distanceKm <= 5) return 1.0;
        if (distanceKm <= 15) return 2.0;
        if (distanceKm <= 30) return 3.5;
        double fee = 3.5 + 0.1 * (distanceKm - 30);
        if (fee > 10) return 10;
        return fee;
    }
}
