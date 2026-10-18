package com.example.fbservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Legacy browser icon URL; redirect to a real same-origin static image. */
@Controller
public class FaviconController {
    @GetMapping("/favicon.ico")
    public String favicon() {
        return "redirect:/favicon.svg";
    }
}
