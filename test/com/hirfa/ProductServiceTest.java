package com.hirfa;

import com.hirfa.model.Outcome;
import com.hirfa.model.Product;
import com.hirfa.repo.Repo;
import com.hirfa.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductServiceTest {
    private ProductService service;
    private Repo repo;

    @BeforeEach
    void setUp() {
        repo = new Repo(":memory:");
        repo.seed();
        service = new ProductService(repo);
    }

    @Test
    void add_validProduct_created() {
        Outcome out = service.add(1, "Copper Ring", "Embroidery", 12.5, 10);
        assertTrue(out.ok());
        assertEquals(6, (int) out.value());
    }

    @Test
    void add_missingName_rejected() {
        assertFalse(service.add(1, "", "Embroidery", 10, 5).ok());
    }

    @Test
    void add_unknownCraft_rejected() {
        assertFalse(service.add(1, "Copper Ring", "Paper Goods", 10, 5).ok());
    }

    @Test
    void add_craftDifferentFromArtisanProfile_rejected() {
        assertFalse(service.add(1, "Laurel Soap", "Soap", 10, 5).ok());
    }

    @Test
    void add_zeroPrice_rejected() {
        Outcome out = service.add(1, "Copper Ring", "Embroidery", 0, 5);
        assertFalse(out.ok());
        assertTrue(out.message().contains("greater than 0"));
    }

    @Test
    void add_overMaxPrice_rejected() {
        Outcome out = service.add(1, "Copper Ring", "Embroidery", 1000.01, 5);
        assertFalse(out.ok());
        assertTrue(out.message().contains("1000 JOD"));
    }

    @Test
    void add_negativeStock_rejected() {
        assertFalse(service.add(1, "Copper Ring", "Embroidery", 10, -1).ok());
    }

    @Test
    void add_overMaxStock_rejected() {
        assertFalse(service.add(1, "Copper Ring", "Embroidery", 10, 501).ok());
    }

    @Test
    void add_nullCraft_rejected() {
        assertFalse(service.add(1, "Copper Ring", null, 10, 5).ok());
    }

    @Test
    void restock_validAmount_stockUpdated() {
        Outcome out = service.restock(4, 20);
        assertTrue(out.ok());
        assertEquals(20, (int) out.value());
    }

    @Test
    void restock_unknownProduct_rejected() {
        Outcome out = service.restock(99, 5);
        assertFalse(out.ok());
        assertTrue(out.message().contains("not found"));
    }

    @Test
    void restock_zeroAmount_rejected() {
        assertFalse(service.restock(4, 0).ok());
    }

    @Test
    void restock_exceedingMax_rejected() {
        assertFalse(service.restock(3, 496).ok());
    }

    @Test
    void restock_integerOverflow_rejected() {
        assertFalse(service.restock(3, Integer.MAX_VALUE).ok());
    }

    @Test
    void search_byCraftOnly_returnsOnlyThatCraft() {
        List<Product> out = service.search("Soap", null);
        assertEquals(2, out.size());
        assertTrue(out.stream().allMatch(p -> p.craft.equals("Soap")));
    }

    @Test
    void search_allWithMaxPrice_filtered() {
        List<Product> out = service.search("", 10.0);
        assertTrue(out.stream().allMatch(p -> p.price <= 10));
        assertEquals(2, out.size());
    }

    @Test
    void search_craftNull_returnsAll() {
        assertEquals(5, service.search(null, null).size());
    }

    @Test
    void search_byCraftAndPrice_combined() {
        List<Product> out = service.search("Embroidery", 20.0);
        assertEquals(1, out.size());
        assertEquals("Embroidered Chair Cover", out.get(0).name);
    }
}
