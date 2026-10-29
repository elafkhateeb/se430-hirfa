package com.hirfa.model;

public class Customer {
    public final int id;
    public String name;
    public String phone;
    public String email;
    public String password;
    public double wallet;
    public int ordersCount;

    public Customer(int id, String name, String phone, String email, String password, double wallet, int ordersCount) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.wallet = wallet;
        this.ordersCount = ordersCount;
    }
}
