package com.example.fbservice.repository;

import com.example.fbservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);  // thêm method này
    boolean existsByUsername(String username);       // nếu cần check tồn tại
}
