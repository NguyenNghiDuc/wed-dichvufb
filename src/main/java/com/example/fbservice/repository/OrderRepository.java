package com.example.fbservice.repository;

import com.example.fbservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);
    Page<Order> findByOwnerUsername(String ownerUsername, Pageable pageable);
    Page<Order> findByOwnerUsernameAndId(String ownerUsername, Long id, Pageable pageable);

    interface OwnerTotals {
        Long getOrderCount();
        Long getAmountVnd();
    }

    /** One grouped aggregate, forced to one authenticated owner. */
    @Query("select count(o.id) as orderCount, coalesce(sum(o.totalPrice), 0L) as amountVnd " +
           "from Order o where o.ownerUsername = :owner")
    OwnerTotals summarizeOwner(@Param("owner") String owner);
}
