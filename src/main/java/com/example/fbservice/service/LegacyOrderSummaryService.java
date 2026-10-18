package com.example.fbservice.service;

import com.example.fbservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyOrderSummaryService {
    private final OrderRepository orders;
    public LegacyOrderSummaryService(OrderRepository orders) { this.orders = orders; }

    @Transactional(readOnly = true)
    public LegacyOrderSummary forOwner(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        OrderRepository.OwnerTotals totals = orders.summarizeOwner(username);
        return new LegacyOrderSummary(totals.getOrderCount(), totals.getAmountVnd());
    }
}
