package com.example.fbservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class HelpController {
    private final HelpFaqService faq;

    public HelpController(HelpFaqService faq) {
        this.faq = faq;
    }

    @GetMapping("/help")
    public String help(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "all") String category,
                       Model model) {
        try {
            var results = faq.search(q, category);
            model.addAttribute("faqs", results);
            model.addAttribute("resultCount", results.size());
            model.addAttribute("topics", faq.topics());
            model.addAttribute("query", q.strip());
            model.addAttribute("selectedCategory", category.isBlank() ? "all" : category.strip());
            return "help";
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid FAQ search filters");
        }
    }
}
