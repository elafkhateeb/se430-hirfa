package com.hirfa;

import com.hirfa.model.Cart;
import com.hirfa.model.Customer;
import com.hirfa.model.Product;
import com.hirfa.repo.Repo;
import com.hirfa.service.OrderService;
import com.hirfa.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReportServiceTest {
    private Repo repo;
    private ReportService report;
    private OrderService orders;
    private LocalDateTime from;
    private LocalDateTime to;

    @BeforeEach
    void setUp() {
        repo = new Repo(":memory:");
        repo.seed();
        repo.insertCustomer(new Customer(0, "Test Customer One", "0791234567",
                "one@example.com", "CustomerPass2026", 100, 0));
        repo.insertCustomer(new Customer(0, "Test Customer Two", "0787654321",
                "two@example.com", "CustomerPass2026", 100, 0));
        report = new ReportService(repo);
        orders = new OrderService(repo);
        from = LocalDateTime.of(2026, 11, 1, 0, 0);
        to = LocalDateTime.of(2026, 11, 30, 23, 59);
    }

    @Test
    void artisanReport_withOrders_sumsRevenue() {
        Cart cart = new Cart();
        cart.put(3, 2);
        cart.put(5, 1);
        orders.place(repo.customer(1), cart, 7, "CASH", "", LocalDateTime.of(2026, 11, 5, 10, 0));
        ReportService.ArtisanReport out = report.artisanReport(2, from, to);
        assertEquals(7.0, out.revenue(), 0.001);
        assertEquals(1, out.ordersCount());
        assertEquals(2, out.itemsCount());
    }

    @Test
    void artisanReport_noOrders_message() {
        ReportService.ArtisanReport out = report.artisanReport(1, from, to);
        assertEquals(0, out.revenue(), 0.001);
        assertEquals(0, out.ordersCount());
        assertEquals("No orders were found in the selected period.", out.message());
    }

    @Test
    void artisanReport_reversedPeriod_rejected() {
        ReportService.ArtisanReport out = report.artisanReport(1, to, from);
        assertEquals(0, out.revenue(), 0.001);
        assertEquals("Invalid report period.", out.message());
    }

    @Test
    void artisanReport_countsOnlyMatchingArtisan() {
        Cart cart = new Cart();
        cart.put(5, 2);
        orders.place(repo.customer(2), cart, 7, "CASH", "", LocalDateTime.of(2026, 11, 6, 9, 0));
        ReportService.ArtisanReport out = report.artisanReport(2, from, to);
        assertEquals(0, out.revenue(), 0.001);
        assertEquals(0, out.ordersCount());
        ReportService.ArtisanReport mine = report.artisanReport(3, from, to);
        assertEquals(90.0, mine.revenue(), 0.001);
        assertEquals(1, mine.ordersCount());
    }
}
