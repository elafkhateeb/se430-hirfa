package com.hirfa.service;

import com.hirfa.model.Outcome;
import com.hirfa.model.Product;
import com.hirfa.repo.Repo;
import java.util.ArrayList;
import java.util.List;

public class ProductService {
    public static final double MAX_PRICE = 1000;
    public static final int MAX_STOCK = 500;
    private final Repo repo;
    private final ValidationService validation = new ValidationService();

    public ProductService(Repo repo) {
        this.repo = repo;
    }

    public Outcome add(int artisanId, String name, String craft, double price, int stock) {
        var artisan = repo.artisan(artisanId);
        if (artisan == null) return Outcome.fail("Artisan not found.");
        if (!validation.validName(name)) return Outcome.fail("Enter a product name using English letters.");
        if (!validation.validCraft(craft)) return Outcome.fail("Choose a supported craft.");
        if (!artisan.craft.equals(craft)) return Outcome.fail("Products must use the artisan's registered craft.");
        if (!Double.isFinite(price) || price <= 0) return Outcome.fail("Price must be greater than 0 JOD.");
        if (price > MAX_PRICE) return Outcome.fail("Price cannot exceed 1000 JOD.");
        if (stock < 0) return Outcome.fail("Stock cannot be negative.");
        if (stock > MAX_STOCK) return Outcome.fail("Stock cannot exceed 500 items.");
        int id = repo.insertProduct(new Product(0, artisanId, name.trim(), craft, price, stock));
        return new Outcome(true, "Product " + name.trim() + " added at " + price + " JOD.", id);
    }

    public Outcome restock(int productId, int amount) {
        Product p = repo.product(productId);
        if (p == null) return Outcome.fail("Product not found.");
        if (amount <= 0) return Outcome.fail("Restock quantity must be positive.");
        if ((long) p.stock + amount > MAX_STOCK) return Outcome.fail("Stock cannot exceed 500 items.");
        p.stock += amount;
        repo.saveProduct(p);
        return new Outcome(true, "Stock updated to " + p.stock + ".", p.stock);
    }

    public List<Product> search(String craft, Double maxPrice) {
        List<Product> out = new ArrayList<>();
        for (Product p : repo.products()) {
            if (craft != null && !craft.isBlank() && !p.craft.equals(craft)) continue;
            if (maxPrice != null && p.price > maxPrice) continue;
            out.add(p);
        }
        return out;
    }
}
