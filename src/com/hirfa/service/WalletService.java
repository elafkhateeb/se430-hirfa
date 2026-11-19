package com.hirfa.service;

import com.hirfa.model.Customer;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;

public class WalletService {
    public static final double MIN_TOPUP = 1;
    public static final double MAX_TOPUP = 1000;
    private final Repo repo;

    public WalletService(Repo repo) {
        this.repo = repo;
    }

    public Outcome topUp(Customer customer, double amount) {
        if (!Double.isFinite(amount) || amount < MIN_TOPUP) return Outcome.fail("Minimum top-up is 1 JOD.");
        if (amount > MAX_TOPUP) return Outcome.fail("Maximum top-up is 1000 JOD.");
        customer.wallet = round(customer.wallet + amount);
        repo.saveCustomer(customer);
        return new Outcome(true, "Wallet topped up. Current balance: " + customer.wallet + " JOD.", customer.wallet);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
