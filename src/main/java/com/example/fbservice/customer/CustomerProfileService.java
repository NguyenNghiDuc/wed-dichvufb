package com.example.fbservice.customer;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerProfileService {
    private final UserRepository users;
    private final CustomerProfileRepository profiles;
    public CustomerProfileService(UserRepository users, CustomerProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public CustomerProfileView view(String owner) {
        User user = existingUser(owner);
        return profiles.findById(owner)
            .map(p -> new CustomerProfileView(user.getUsername(), user.getEmail(),
                p.getDisplayName(), p.getPhone()))
            .orElse(new CustomerProfileView(user.getUsername(), user.getEmail(), "", ""));
    }

    @Transactional
    public CustomerProfileView update(String owner, CustomerProfileRequest request) {
        User user = existingUser(owner);
        String name = request == null || request.displayName() == null ? "" : request.displayName().trim();
        String phone = request == null || request.phone() == null ? "" : request.phone().trim();
        if (name.length() < 2 || name.length() > 80 ||
            name.chars().anyMatch(Character::isISOControl) ||
            (!phone.isEmpty() && (!phone.matches("[+0-9 .()\\-]{8,20}") ||
                                     phone.chars().filter(Character::isDigit).count() < 8)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin liên hệ không hợp lệ.");
        CustomerProfile profile = profiles.findById(owner).orElseGet(() -> new CustomerProfile(owner));
        profile.update(name, phone);
        profiles.save(profile);
        return new CustomerProfileView(user.getUsername(), user.getEmail(), name, phone);
    }

    private User existingUser(String owner) {
        if (owner == null || owner.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByUsername(owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
