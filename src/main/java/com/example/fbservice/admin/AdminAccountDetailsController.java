package com.example.fbservice.admin;

import com.example.fbservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Admin-only read-only detail view; never places a password hash in the model. */
@Controller
public class AdminAccountDetailsController {
    public record AccountSummary(Long id, String username, String role, boolean enabled) {}

    private final UserRepository users;
    private final AdminUserService accounts;

    public AdminAccountDetailsController(UserRepository users, AdminUserService accounts) {
        this.users = users;
        this.accounts = accounts;
    }

    @GetMapping("/admin/users/{id}")
    public String details(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        if (id == null || id <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID");
        var user = users.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Reuse the existing authorization-scoped, DB-filtered 20-event audit paging.
        var events = accounts.pageAudit(page, null, id, null);
        model.addAttribute("account",
            new AccountSummary(user.getId(), user.getUsername(), user.getRole(),
                user.isEnabled()));
        model.addAttribute("events", events.getContent());
        model.addAttribute("eventTotal", events.getTotalElements());
        model.addAttribute("page", page);
        model.addAttribute("hasNext", events.hasNext());
        model.addAttribute("hasPrevious", events.hasPrevious());
        model.addAttribute("totalPages", events.getTotalPages());
        return "admin_user_details";
    }
}
