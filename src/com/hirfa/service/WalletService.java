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
        if (amount < MIN_TOPUP) return Outcome.fail("أقل قيمة شحن هي دينار واحد");
        if (amount > MAX_TOPUP) return Outcome.fail("أقصى قيمة شحن هي 1000 دينار");
        customer.wallet = round(customer.wallet + amount);
        repo.saveCustomer(customer);
        return new Outcome(true, "تم شحن المحفظة، الرصيد الحالي " + customer.wallet + " دينار", customer.wallet);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
