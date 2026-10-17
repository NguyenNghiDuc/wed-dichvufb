package com.example.fbservice.service;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.existsByUsername(username);
    }

    @Override
    public User saveUser(User user) {
        user.setId(null);
        user.setRole("USER"); // User-controlled registration must never grant ADMIN.
        user.setPassword(encoder.encode(user.getPassword()));
        return users.save(user);
    }
}
