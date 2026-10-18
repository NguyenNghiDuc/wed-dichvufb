package com.example.fbservice.tiktok;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TiktokOrderEventRepository extends JpaRepository<TiktokOrderEvent,Long> {
    Page<TiktokOrderEvent> findByOrderIdOrderByCreatedAtDescIdDesc(Long id,Pageable page);
}
