package com.example.fbservice.repository;

import com.example.fbservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);
}
