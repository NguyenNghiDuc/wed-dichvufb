package com.example.fbservice.repository;

import com.example.fbservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);
    Page<Order> findByOwnerUsername(String ownerUsername, Pageable pageable);
}
