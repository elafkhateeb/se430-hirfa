package com.hirfa;

import com.hirfa.model.Cart;
import com.hirfa.model.Customer;
import com.hirfa.repo.Repo;
import com.hirfa.service.OrderService;
import com.hirfa.web.WebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebServerTest {
    private Repo repo;
    private WebServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        repo = new Repo(":memory:");
        repo.seed();
        repo.insertCustomer(new Customer(0, "Amal Al-Khatib", "0791234567",
                "customer@example.com", "CustomerPass2026", 25, 3));
        server = new WebServer(repo);
        server.start(0);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        server.close();
        repo.close();
    }

    @Test
    void artisanRegistrationCreatesAccountAndEnablesProductManagement() throws Exception {
        HttpResponse<String> registration = post("/register", Map.of(
                "role", "artisan",
                "name", "Nadia Qadri",
                "phone", "0793344556",
                "email", "nadia@example.com",
                "password", "NadiaPass2026",
                "craft", "Embroidery"), null);
        assertEquals(200, registration.statusCode());
        assertTrue(registration.body().contains("Artisan account created"));
        String cookie = cookie(registration);

        HttpResponse<String> addProduct = post("/artisan/dashboard", Map.of(
                "action", "add",
                "name", "Handmade Scarf",
                "price", "18.50",
                "stock", "7"), cookie);
        assertEquals(200, addProduct.statusCode());
        assertTrue(addProduct.body().contains("Artisan dashboard"));

        HttpResponse<String> catalog = get("/products", cookie);
        assertTrue(catalog.body().contains("Handmade Scarf"));
    }

    @Test
    void artisanCannotRestockAnotherArtisansProduct() throws Exception {
        HttpResponse<String> registration = post("/register", Map.of(
                "role", "artisan",
                "name", "Nadia Qadri",
                "phone", "0793344556",
                "email", "nadia@example.com",
                "password", "NadiaPass2026",
                "craft", "Embroidery"), null);
        String cookie = cookie(registration);

        HttpResponse<String> response = post("/artisan/dashboard", Map.of(
                "action", "restock",
                "productId", "1",
                "qty", "5"), cookie);
        assertTrue(response.body().contains("Product not found in your account."));
        assertEquals(8, repo.product(1).stock);
    }

    @Test
    void customerLoginHidesAuthenticationLinksAndDemoHint() throws Exception {
        HttpResponse<String> response = post("/login", Map.of(
                "role", "customer",
                "email", "customer@example.com",
                "password", "CustomerPass2026"), null);
        assertTrue(response.body().contains("Log out"));
        assertFalse(response.body().contains("Demo member:"));
        assertFalse(response.body().contains("No account?"));
    }

    @Test
    void customerCanCancelNewOrderAndRestoreStock() throws Exception {
        Cart cart = new Cart();
        cart.set(3, 2);
        OrderService orders = new OrderService(repo);
        assertTrue(orders.place(repo.customer(1), cart, 7, "CASH", "", LocalDateTime.now()).ok());

        HttpResponse<String> login = post("/login", Map.of(
                "role", "customer",
                "email", "customer@example.com",
                "password", "CustomerPass2026"), null);
        HttpResponse<String> cancel = post("/orders", Map.of("orderId", "1"), cookie(login));

        assertTrue(cancel.body().contains("Order cancelled."));
        assertEquals("CANCELLED", repo.order(1).status.name());
        assertEquals(30, repo.product(3).stock);
    }

    @Test
    void cancellingWalletOrderRefundsBalance() throws Exception {
        Cart cart = new Cart();
        cart.set(3, 2);
        OrderService orders = new OrderService(repo);
        assertTrue(orders.place(repo.customer(1), cart, 7, "WALLET", "", LocalDateTime.now()).ok());
        assertEquals(16, repo.customer(1).wallet, 0.001);

        HttpResponse<String> login = post("/login", Map.of(
                "role", "customer",
                "email", "customer@example.com",
                "password", "CustomerPass2026"), null);
        HttpResponse<String> cancel = post("/orders", Map.of("orderId", "1"), cookie(login));

        assertTrue(cancel.body().contains("refunded to the wallet"));
        assertEquals(25, repo.customer(1).wallet, 0.001);
        assertEquals(30, repo.product(3).stock);
    }

    @Test
    void customerCanUpdateAndRemoveCartItems() throws Exception {
        String cookie = customerSession();
        HttpResponse<String> added = post("/cart", Map.of(
                "action", "add",
                "productId", "3",
                "qty", "2"), cookie);
        assertTrue(added.body().contains("Added 2 of Laurel Oil Soap"));

        HttpResponse<String> updated = post("/cart", Map.of(
                "action", "update",
                "productId", "3",
                "qty", "4"), cookie);
        assertTrue(updated.body().contains("Cart quantity for Laurel Oil Soap is now 4"));
        assertTrue(updated.body().contains("14.00 JOD"));

        HttpResponse<String> removed = post("/cart", Map.of(
                "action", "remove",
                "productId", "3"), cookie);
        assertTrue(removed.body().contains("Item removed from your cart."));
        assertTrue(removed.body().contains("Your cart is empty"));
    }

    @Test
    void checkoutValidatesCardAndClearsCartAfterSuccessfulPayment() throws Exception {
        String cookie = customerSession();
        post("/cart", Map.of("action", "add", "productId", "3", "qty", "2"), cookie);

        HttpResponse<String> invalidPayment = post("/order", Map.of(
                "km", "7",
                "payment", "CARD",
                "card", "1234",
                "promo", ""), cookie);
        assertTrue(invalidPayment.body().contains("Card number must be exactly 16 digits."));
        assertTrue(repo.ordersByCustomer(1).isEmpty());

        HttpResponse<String> placed = post("/order", Map.of(
                "km", "7",
                "payment", "CARD",
                "card", "1234567890123456",
                "promo", ""), cookie);
        assertTrue(placed.body().contains("Order #1 created"));
        assertEquals(1, repo.ordersByCustomer(1).size());
        assertTrue(get("/cart", cookie).body().contains("Your cart is empty"));
    }

    private String customerSession() throws Exception {
        HttpResponse<String> login = post("/login", Map.of(
                "role", "customer",
                "email", "customer@example.com",
                "password", "CustomerPass2026"), null);
        return cookie(login);
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (cookie != null) request.header("Cookie", cookie);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Map<String, String> fields, String cookie) throws Exception {
        String body = fields.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) request.header("Cookie", cookie);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.port() + path);
    }

    private String cookie(HttpResponse<?> response) {
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
