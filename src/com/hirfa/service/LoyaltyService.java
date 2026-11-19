package com.hirfa.service;

public class LoyaltyService {

    public Tier tierFor(int ordersCount) {
        if (ordersCount >= 30) return new Tier("Diamond", 15);
        if (ordersCount >= 15) return new Tier("Gold", 10);
        if (ordersCount >= 5) return new Tier("Silver", 5);
        return new Tier("Bronze", 0);
    }
}
