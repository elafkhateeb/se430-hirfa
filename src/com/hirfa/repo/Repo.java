package com.hirfa.repo;

import com.hirfa.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Repo implements AutoCloseable {
    private final Connection conn;

    public Repo(String path) {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, email TEXT UNIQUE, password TEXT, wallet REAL, orders_count INTEGER)");
                st.execute("CREATE TABLE IF NOT EXISTS artisans(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, craft TEXT, rating_sum REAL, rating_count INTEGER)");
                st.execute("CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT, artisan_id INTEGER, name TEXT, craft TEXT, price REAL, stock INTEGER)");
                st.execute("CREATE TABLE IF NOT EXISTS orders(id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER, payment TEXT, distance REAL, created_at TEXT, status TEXT, subtotal REAL, fee REAL, discount REAL, total REAL)");
                st.execute("CREATE TABLE IF NOT EXISTS order_items(order_id INTEGER, position INTEGER, product_id INTEGER, name TEXT, unit_price REAL, qty INTEGER)");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void seed() {
        if (!queryList("SELECT 1 FROM customers LIMIT 1", rs -> 1).isEmpty()) return;
        int amal = insertCustomer(new Customer(0, "أمل الخطيب", "0791234567", "amal@gmail.com", "Amal@2024", 25, 3));
        int reem = insertCustomer(new Customer(0, "ريم الشوبكي", "0787654321", "reem@gmail.com", "Reem#2024", 10, 2));
        int sumayya = insertArtisan(new Artisan(0, "سمية المجالي", "تطريز", 12, 3));
        int liyan = insertArtisan(new Artisan(0, "ليان الزعبي", "صابون", 9, 2));
        int mohammad = insertArtisan(new Artisan(0, "محمد العمري", "خشبيات", 5, 1));
        insertProduct(new Product(0, sumayya, "مفرش كرسي مطرز", "تطريز", 15, 8));
        insertProduct(new Product(0, sumayya, "كوسطة تطريز فلاحي", "تطريز", 22.5, 4));
        insertProduct(new Product(0, liyan, "صابون زيت الغار", "صابون", 3.5, 30));
        insertProduct(new Product(0, liyan, "صابون حبة البركة", "صابون", 4, 0));
        insertProduct(new Product(0, mohammad, "مجسم خشبي منحوت", "خشبيات", 45, 6));
    }

    public int insertCustomer(Customer c) {
        return insert("INSERT INTO customers(name,phone,email,password,wallet,orders_count) VALUES(?,?,?,?,?,?)",
                c.name, c.phone, c.email, c.password, c.wallet, c.ordersCount);
    }

    public int insertArtisan(Artisan a) {
        return insert("INSERT INTO artisans(name,craft,rating_sum,rating_count) VALUES(?,?,?,?)",
                a.name, a.craft, a.ratingSum, a.ratingCount);
    }

    public int insertProduct(Product p) {
        return insert("INSERT INTO products(artisan_id,name,craft,price,stock) VALUES(?,?,?,?,?)",
                p.artisanId, p.name, p.craft, p.price, p.stock);
    }

    public void saveProduct(Product p) {
        update("UPDATE products SET name=?, craft=?, price=?, stock=? WHERE id=?", p.name, p.craft, p.price, p.stock, p.id);
    }

    public void saveCustomer(Customer c) {
        update("UPDATE customers SET name=?, phone=?, email=?, password=?, wallet=?, orders_count=? WHERE id=?",
                c.name, c.phone, c.email, c.password, c.wallet, c.ordersCount, c.id);
    }

    public Customer customer(int id) {
        return queryOne("SELECT * FROM customers WHERE id=?", Repo::customerMapper, id);
    }

    public Customer customerByEmail(String email) {
        return queryOne("SELECT * FROM customers WHERE email=?", Repo::customerMapper, email);
    }

    private static Customer customerMapper(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("id"), rs.getString("name"), rs.getString("phone"),
                rs.getString("email"), rs.getString("password"), rs.getDouble("wallet"), rs.getInt("orders_count"));
    }

    public Artisan artisan(int id) {
        return queryOne("SELECT * FROM artisans WHERE id=?", Repo::artisanMapper, id);
    }

    private static Artisan artisanMapper(ResultSet rs) throws SQLException {
        return new Artisan(rs.getInt("id"), rs.getString("name"), rs.getString("craft"),
                rs.getDouble("rating_sum"), rs.getInt("rating_count"));
    }

    public void rateArtisan(int artisanId, int stars) {
        update("UPDATE artisans SET rating_sum=rating_sum+?, rating_count=rating_count+1 WHERE id=?", stars, artisanId);
    }

    public Product product(int id) {
        return queryOne("SELECT * FROM products WHERE id=?", Repo::productMapper, id);
    }

    public List<Product> products() {
        return queryList("SELECT * FROM products ORDER BY id", Repo::productMapper);
    }

    public List<Product> productsByCraft(String craft) {
        return queryList("SELECT * FROM products WHERE craft=? ORDER BY id", Repo::productMapper, craft);
    }

    private static Product productMapper(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("id"), rs.getInt("artisan_id"), rs.getString("name"),
                rs.getString("craft"), rs.getDouble("price"), rs.getInt("stock"));
    }

    public int createOrder(Order o) {
        int id = insert("INSERT INTO orders(customer_id,payment,distance,created_at,status,subtotal,fee,discount,total) VALUES(?,?,?,?,?,?,?,?,?)",
                o.customerId, o.payment, o.distanceKm, o.createdAt.toString(), o.status.name(), o.subtotal, o.fee, o.discount, o.total);
        List<OrderItem> items = o.items;
        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            update("INSERT INTO order_items(order_id,position,product_id,name,unit_price,qty) VALUES(?,?,?,?,?,?)",
                    id, i + 1, it.productId(), it.name(), it.unitPrice(), it.qty());
        }
        return id;
    }

    public Order order(int id) {
        List<OrderItem> items = queryList("SELECT * FROM order_items WHERE order_id=? ORDER BY position",
                rs -> {
                    try {
                        return new OrderItem(rs.getInt("product_id"), rs.getString("name"), rs.getDouble("unit_price"), rs.getInt("qty"));
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                }, id);
        return queryOne("SELECT * FROM orders WHERE id=?", rs -> {
            try {
                return new Order(id, rs.getInt("customer_id"), items, rs.getString("payment"), rs.getDouble("distance"),
                        LocalDateTime.parse(rs.getString("created_at")), rs.getDouble("subtotal"), rs.getDouble("fee"),
                        rs.getDouble("discount"), rs.getDouble("total"), Order.Status.valueOf(rs.getString("status")));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }, id);
    }

    public void updateStatus(int orderId, Order.Status status) {
        update("UPDATE orders SET status=? WHERE id=?", status.name(), orderId);
    }

public List<Artisan> artisans() {
        return queryList("SELECT * FROM artisans ORDER BY id", Repo::artisanMapper);
    }

    public List<Order> ordersByCustomer(int customerId) {
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM orders WHERE customer_id=? ORDER BY id DESC")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(order(rs.getInt(1)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return list;
    }

    public List<Integer> orderIdsBetween(LocalDateTime from, LocalDateTime to) {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM orders WHERE created_at>=? AND created_at<=? ORDER BY id")) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return ids;
    }

    private int insert(String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public int update(String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T queryOne(String sql, SqlMapper<T> m, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? m.map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> List<T> queryList(String sql, SqlMapper<T> m, Object... params) {
        List<T> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(m.map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    @FunctionalInterface
    private interface SqlMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
