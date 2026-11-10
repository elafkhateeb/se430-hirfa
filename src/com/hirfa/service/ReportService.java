package com.hirfa.service;

import com.hirfa.model.Order;
import com.hirfa.model.OrderItem;
import com.hirfa.repo.Repo;
import java.time.LocalDateTime;

public class ReportService {
    private final Repo repo;

    public ReportService(Repo repo) {
        this.repo = repo;
    }

    public record ArtisanReport(double revenue, int ordersCount, int itemsCount, String message) {
    }

    public ArtisanReport artisanReport(int artisanId, LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) return new ArtisanReport(0, 0, 0, "الفترة غير صحيحة");
        double revenue = 0;
        int ordersCount = 0;
        int itemsCount = 0;
        for (int id : repo.orderIdsBetween(from, to)) {
            Order o = repo.order(id);
            boolean artisanInOrder = false;
            for (OrderItem it : o.items) {
                if (repo.product(it.productId()).artisanId == artisanId) {
                    revenue += it.unitPrice() * it.qty();
                    itemsCount += it.qty();
                    artisanInOrder = true;
                }
            }
            if (artisanInOrder) ordersCount++;
        }
        if (ordersCount == 0) return new ArtisanReport(0, 0, 0, "لا توجد طلبات ضمن الفترة المحددة");
        String msg = "إيراد " + revenue + " دينار من " + ordersCount + " طلب";
        return new ArtisanReport(revenue, ordersCount, itemsCount, msg);
    }
}
