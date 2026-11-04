package com.hirfa;

import com.hirfa.model.Cart;
import com.hirfa.model.Customer;
import com.hirfa.model.Order;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;
import com.hirfa.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderServiceTest {
    private OrderService service;
    private Repo repo;
    private Cart cart;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        repo = new Repo(":memory:");
        repo.seed();
        service = new OrderService(repo);
        cart = new Cart();
        now = LocalDateTime.of(2026, 11, 8, 12, 0);
    }

    @Test
    void place_happyPath_createsOrderAndUpdatesStock() {
        cart.put(3, 2);
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "", now);
        assertTrue(out.ok());
        assertTrue(out.message().contains("تم إنشاء الطلب رقم"));
        assertEquals(28, repo.product(3).stock);
    }

    @Test
    void place_emptyCart_rejected() {
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "", now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("السلة فارغة"));
    }

    @Test
    void place_belowMinTotal_rejected() {
        cart.put(3, 1);
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "", now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("الحد الأدنى"));
    }

    @Test
    void place_stockShortage_rejected() {
        cart.put(4, 1);
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "", now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("لم يعد متوفراً"));
    }

    @Test
    void place_unknownPaymentMethod_rejected() {
        cart.put(3, 2);
        Outcome out = service.place(repo.customer(1), cart, 7, "BITCOIN", "", now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير معتمدة"));
    }

    @Test
    void place_nullPaymentMethod_rejected() {
        cart.put(3, 2);
        assertFalse(service.place(repo.customer(1), cart, 7, null, "", now).ok());
    }

    @Test
    void place_cardPayment_ok() {
        cart.put(3, 2);
        Outcome out = service.place(repo.customer(1), cart, 7, "CARD", "", now);
        assertTrue(out.ok());
    }

    @Test
    void place_invalidPromo_rejected() {
        cart.put(3, 2);
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "خصم50", now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير معتمد"));
    }

    @Test
    void place_ghostProduct_rejected() {
        cart.put(99, 1);
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "", now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("لم يعد متوفراً"));
    }

    @Test
    void place_walletInsufficient_rejected() {
        cart.put(5, 1);
        Outcome out = service.place(repo.customer(2), cart, 10, "WALLET", "", now);
        assertFalse(out.ok());
        assertTrue(out.message().contains("غير كافٍ"));
    }

    @Test
    void place_withPromo_totalsCorrect() {
        cart.put(1, 2);
        cart.put(3, 3);
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "حرفة10", now);
        assertTrue(out.ok());
        Order order = repo.order((int) out.value());
        assertEquals(40.5, order.subtotal, 0.001);
        assertEquals(2.0, order.fee, 0.001);
        assertEquals(4.05, order.discount, 0.001);
        assertEquals(38.45, order.total, 0.001);
    }

    @Test
    void place_freeShipping_over100() {
        cart.put(5, 2);
        cart.put(1, 1);
        Outcome out = service.place(repo.customer(1), cart, 7, "CASH", "", now);
        assertTrue(out.ok());
        Order order = repo.order((int) out.value());
        assertEquals(105.0, order.subtotal, 0.001);
        assertEquals(0.0, order.fee, 0.001);
        assertEquals(105.0, order.total, 0.001);
    }

    @Test
    void place_walletPayment_deductsBalance() {
        cart.put(3, 2);
        Outcome out = service.place(repo.customer(1), cart, 7, "WALLET", "", now);
        assertTrue(out.ok());
        Customer after = repo.customer(1);
        assertEquals(16.0, after.wallet, 0.001);
        assertEquals(4, after.ordersCount);
    }
}
