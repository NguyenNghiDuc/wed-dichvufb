package com.example.fbservice.service;

import com.example.fbservice.model.Order;
import com.example.fbservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    public Order create(Order input, String username) {
        if (input.getQuantity() < 1 || input.getQuantity() > 10000
            || input.getLinkOrder() == null || input.getLinkOrder().isBlank()
            || input.getLinkOrder().length() > 255) {
            throw new IllegalArgumentException("Thông tin đơn hàng không hợp lệ.");
        }
        Order order = new Order();
        order.setOwnerUsername(username);
        order.setLinkOrder(input.getLinkOrder().trim());
        order.setServerName(input.getServerName());
        order.setQuantity(input.getQuantity());
        order.setNote(input.getNote());
        order.setPricePerUnit(10); // Server price: never accept client-supplied unit price.
        return orders.save(order);
    }

    public List<Order> list(String username) {
        return orders.findByOwnerUsernameOrderByCreatedAtDesc(username);
    }
}
