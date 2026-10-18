package com.example.fbservice.repository;

import com.example.fbservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);
    Page<Order> findByOwnerUsername(String ownerUsername, Pageable pageable);
    Page<Order> findByOwnerUsernameAndId(String ownerUsername, Long id, Pageable pageable);
}
