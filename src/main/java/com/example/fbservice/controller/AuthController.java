package com.example.fbservice.controller;

import com.example.fbservice.model.User;
import com.example.fbservice.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(required = false) String email,
                               Model model) {
        String cleanName = username == null ? "" : username.trim();
        if (!cleanName.matches("[a-zA-Z0-9._-]{3,40}")) {
            model.addAttribute("error", "Tên tài khoản cần 3–40 ký tự chữ, số, dấu chấm hoặc gạch.");
            return "register";
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            model.addAttribute("error", "Mật khẩu cần từ 8 đến 72 ký tự.");
            return "register";
        }
        if (userService.existsByUsername(cleanName)) {
            model.addAttribute("error", "Tên tài khoản đã tồn tại.");
            return "register";
        }
        User user = new User();
        user.setUsername(cleanName);
        user.setPassword(password);
        user.setEmail(email == null || email.isBlank() ? null : email.trim());
        try {
            userService.saveUser(user);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "Tài khoản không hợp lệ hoặc đã tồn tại.");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
