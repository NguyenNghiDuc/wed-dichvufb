package com.example.fbservice.controller;

import com.example.fbservice.model.Order;
import com.example.fbservice.service.OrderService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/order")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("order", new Order());
        return "order_form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Order order, Principal principal, Model model) {
        try {
            orders.create(order, principal.getName());
            return "redirect:/order/list";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "order_form";
        }
    }

    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        model.addAttribute("orders", orders.list(principal.getName()));
        return "order_list";
    }
}
