package com.hirfa;

import com.hirfa.model.Cart;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;
import com.hirfa.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CartServiceTest {
    private CartService service;
    private Cart cart;

    @BeforeEach
    void setUp() {
        Repo repo = new Repo(":memory:");
        repo.seed();
        service = new CartService(repo);
        cart = new Cart();
    }

    @Test
    void addItem_valid_added() {
        Outcome out = service.addItem(cart, 3, 2);
        assertTrue(out.ok());
        assertEquals(2, cart.qtyOf(3));
    }

    @Test
    void addItem_mergesQuantities() {
        service.addItem(cart, 3, 2);
        service.addItem(cart, 3, 1);
        assertEquals(3, cart.qtyOf(3));
    }

    @Test
    void addItem_rejectsTotalAbovePerItemLimit() {
        service.addItem(cart, 3, 2);
        Outcome result = service.addItem(cart, 3, 4);
        assertFalse(result.ok());
        assertEquals(2, cart.qtyOf(3));
    }

    @Test
    void addItem_unknownProduct_rejected() {
        Outcome out = service.addItem(cart, 99, 1);
        assertFalse(out.ok());
        assertTrue(out.message().contains("not found"));
    }

    @Test
    void addItem_zeroQty_rejected() {
        Outcome out = service.addItem(cart, 3, 0);
        assertFalse(out.ok());
        assertTrue(out.message().contains("positive"));
    }

    @Test
    void addItem_sixUnits_rejected() {
        Outcome out = service.addItem(cart, 3, 6);
        assertFalse(out.ok());
        assertTrue(out.message().contains("up to 5"));
    }

    @Test
    void addItem_outOfStockProduct_rejected() {
        Outcome out = service.addItem(cart, 4, 1);
        assertFalse(out.ok());
        assertTrue(out.message().contains("out of stock"));
    }

    @Test
    void changeQty_valid_updatesCart() {
        service.addItem(cart, 3, 2);
        Outcome out = service.changeQty(cart, 3, 4);
        assertTrue(out.ok());
        assertEquals(4, cart.qtyOf(3));
    }

    @Test
    void changeQty_overFive_rejected() {
        service.addItem(cart, 3, 2);
        assertFalse(service.changeQty(cart, 3, 6).ok());
    }

    @Test
    void changeQty_zero_rejected() {
        service.addItem(cart, 3, 2);
        assertFalse(service.changeQty(cart, 3, 0).ok());
    }

    @Test
    void changeQty_overStock_rejected() {
        service.addItem(cart, 1, 1);
        assertFalse(service.changeQty(cart, 1, 9).ok());
    }

    @Test
    void changeQty_qtyExceedsAvailableStock_rejected() {
        assertFalse(service.changeQty(cart, 2, 5).ok());
    }

    @Test
    void changeQty_unknownProduct_rejected() {
        service.addItem(cart, 3, 2);
        assertFalse(service.changeQty(cart, 99, 2).ok());
    }

    @Test
    void total_sumsPrices() {
        service.addItem(cart, 3, 2);
        service.addItem(cart, 1, 1);
        assertEquals(22.0, service.total(cart), 0.001);
    }
}
