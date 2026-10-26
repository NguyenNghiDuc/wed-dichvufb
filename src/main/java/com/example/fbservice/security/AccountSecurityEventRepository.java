package com.example.fbservice.security;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountSecurityEventRepository extends JpaRepository<AccountSecurityEvent,Long>{
 Page<AccountSecurityEvent> findByUserId(Long userId,Pageable page);
}
