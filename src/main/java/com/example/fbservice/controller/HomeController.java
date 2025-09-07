package com.example.fbservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Xin chào! Trang chủ đã chạy OK");
        return "home"; // sẽ render home.html trong templates
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // render login.html
    }

    @GetMapping("/register")
    public String register() {
        return "register"; // render register.html
    }
}
