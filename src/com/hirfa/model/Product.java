package com.hirfa.model;

public class Product {
    public final int id;
    public final int artisanId;
    public String name;
    public String craft;
    public double price;
    public int stock;

    public Product(int id, int artisanId, String name, String craft, double price, int stock) {
        this.id = id;
        this.artisanId = artisanId;
        this.name = name;
        this.craft = craft;
        this.price = price;
        this.stock = stock;
    }
}
