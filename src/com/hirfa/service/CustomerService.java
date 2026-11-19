package com.hirfa.service;

import com.hirfa.model.Customer;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;

public class CustomerService {
    public static final double MIN_PASSWORD_LENGTH = 8;
    private final Repo repo;
    private final ValidationService validation;

    public CustomerService(Repo repo) {
        this.repo = repo;
        this.validation = new ValidationService();
    }

    public Outcome register(String name, String phone, String email, String password) {
        if (!validation.validName(name)) return Outcome.fail("Enter a valid name using English letters.");
        if (!validation.validPhone(phone)) return Outcome.fail("Enter a valid Jordanian mobile number.");
        if (!validation.validEmail(email)) return Outcome.fail("Enter a valid email address.");
        if (!validation.validPassword(password)) return Outcome.fail("Password must contain at least 8 characters, letters, and numbers.");
        String normalizedEmail = email.trim().toLowerCase();
        if (repo.customerByEmail(normalizedEmail) != null) return Outcome.fail("This email is already registered.");
        int id = repo.insertCustomer(new Customer(0, name.trim(), phone, normalizedEmail, password, 0, 0));
        return new Outcome(true, "Welcome, " + name.trim() + "! Your account has been created.", id);
    }

    public Outcome login(String email, String password) {
        Customer c = email == null ? null : repo.customerByEmail(email.trim().toLowerCase());
        if (c == null) return Outcome.fail("Email is not registered.");
        if (!c.password.equals(password)) return Outcome.fail("Incorrect password.");
        return Outcome.ok("Welcome back, " + c.name + ".", c.id);
    }
}
