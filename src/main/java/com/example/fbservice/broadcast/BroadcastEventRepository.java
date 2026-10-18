package com.example.fbservice.broadcast;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BroadcastEventRepository extends JpaRepository<BroadcastEvent,Long>{List<BroadcastEvent> findByBroadcastIdOrderByCreatedAtAscIdAsc(Long id);}
