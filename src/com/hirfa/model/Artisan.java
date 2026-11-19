package com.hirfa.model;

public class Artisan {
    public final int id;
    public String name;
    public String craft;
    public String email;
    public String password;
    public double ratingSum;
    public int ratingCount;

    public Artisan(int id, String name, String craft, double ratingSum, int ratingCount) {
        this(id, name, craft, null, null, ratingSum, ratingCount);
    }

    public Artisan(int id, String name, String craft, String email, String password,
                   double ratingSum, int ratingCount) {
        this.id = id;
        this.name = name;
        this.craft = craft;
        this.email = email;
        this.password = password;
        this.ratingSum = ratingSum;
        this.ratingCount = ratingCount;
    }

    public double rating() {
        return ratingCount == 0 ? 0 : Math.round((ratingSum / ratingCount) * 10.0) / 10.0;
    }
}
