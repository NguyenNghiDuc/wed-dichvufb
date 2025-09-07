package com.example.fbservice.service;

import com.example.fbservice.model.User;

public interface UserService {
    boolean existsByUsername(String username);
    User saveUser(User user);
}
