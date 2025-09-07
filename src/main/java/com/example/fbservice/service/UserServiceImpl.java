package com.example.fbservice.service;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
