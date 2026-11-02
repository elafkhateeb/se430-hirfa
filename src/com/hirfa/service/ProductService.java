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
        if (!validation.validName(name)) return Outcome.fail("اسم المنتج مطلوب");
        if (!validation.validCraft(craft)) return Outcome.fail("الفئة غير معتمدة");
        if (price <= 0) return Outcome.fail("السعر يجب أن يكون أكبر من صفر");
        if (price > MAX_PRICE) return Outcome.fail("السعر يتجاوز الحد الأقصى 1000 دينار");
        if (stock < 0) return Outcome.fail("المخزون لا يمكن أن يكون سالباً");
        if (stock > MAX_STOCK) return Outcome.fail("المخزون يتجاوز الحد الأقصى 500 قطعة");
        int id = repo.insertProduct(new Product(0, artisanId, name.trim(), craft, price, stock));
        return new Outcome(true, "تم إضافة المنتج " + name.trim() + " بسعر " + price + " دينار", id);
    }

    public Outcome restock(int productId, int amount) {
        Product p = repo.product(productId);
        if (p == null) return Outcome.fail("المنتج غير موجود");
        if (amount <= 0) return Outcome.fail("الكمية غير صحيحة");
        if (p.stock + amount > MAX_STOCK) return Outcome.fail("المخزون يتجاوز الحد الأقصى 500 قطعة");
        p.stock += amount;
        repo.saveProduct(p);
        return new Outcome(true, "تم إعادة التخزين، الكمية الحالية " + p.stock, p.stock);
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
