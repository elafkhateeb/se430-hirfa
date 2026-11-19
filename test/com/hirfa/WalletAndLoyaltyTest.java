package com.hirfa;

import com.hirfa.model.Customer;
import com.hirfa.model.Outcome;
import com.hirfa.repo.Repo;
import com.hirfa.service.LoyaltyService;
import com.hirfa.service.Tier;
import com.hirfa.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletAndLoyaltyTest {
    private Repo repo;
    private WalletService wallet;
    private int customerId;
    private final LoyaltyService loyalty = new LoyaltyService();

    @BeforeEach
    void setUp() {
        repo = new Repo(":memory:");
        repo.seed();
        wallet = new WalletService(repo);
        customerId = repo.insertCustomer(new Customer(0, "Test Wallet Customer", "0787654321",
                "wallet@example.com", "WalletPass2026", 10, 0));
    }

    private Customer customer() {
        return repo.customer(customerId);
    }

    @Test
    void topUp_valid_balanceIncreased() {
        Outcome out = wallet.topUp(customer(), 50);
        assertTrue(out.ok());
        assertEquals(60, out.value(), 0.001);
    }

    @Test
    void topUp_belowOne_rejected() {
        Outcome out = wallet.topUp(customer(), 0.99);
        assertFalse(out.ok());
        assertTrue(out.message().contains("1 JOD"));
    }

    @Test
    void topUp_overMax_rejected() {
        Outcome out = wallet.topUp(customer(), 1001);
        assertFalse(out.ok());
        assertTrue(out.message().contains("1000"));
    }

    @Test
    void topUp_decimalRounds() {
        Outcome out = wallet.topUp(customer(), 1.111);
        assertEquals(11.11, out.value(), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
            "0, Bronze, 0",
            "4, Bronze, 0",
            "5, Silver, 5",
            "14, Silver, 5",
            "15, Gold, 10",
            "29, Gold, 10",
            "30, Diamond, 15",
            "99, Diamond, 15"
    })
    void tierFor_boundaries(int orders, String name, double percent) {
        Tier tier = loyalty.tierFor(orders);
        assertEquals(name, tier.name());
        assertEquals(percent, tier.discountPercent(), 0.001);
    }
}
