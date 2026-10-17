package com.example.fbservice.repository;

import com.example.fbservice.model.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<User> findAllByOrderByIdDesc(Pageable pageable);
    long countByEnabledTrue();
    List<User> findTop100ByEnabledTrueAndRoleInOrderByUsernameAsc(List<String> roles);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = 'ADMIN' and u.enabled = true order by u.id")
    List<User> lockEnabledAdmins();
}
