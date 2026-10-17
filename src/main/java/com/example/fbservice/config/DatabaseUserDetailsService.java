package com.example.fbservice.config;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        String role = switch (user.getRole()) {
            case "ADMIN" -> "ADMIN";
            case "STAFF" -> "STAFF";
            default -> "USER";
        };
        return new AccountPrincipal(user, role);
    }
}
