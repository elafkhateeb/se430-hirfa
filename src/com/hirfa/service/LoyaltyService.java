package com.hirfa.service;

public class LoyaltyService {

    public Tier tierFor(int ordersCount) {
        if (ordersCount >= 30) return new Tier("الماسي", 15);
        if (ordersCount >= 15) return new Tier("الذهبي", 10);
        if (ordersCount >= 5) return new Tier("الفضي", 5);
        return new Tier("البرونزي", 0);
    }
}
