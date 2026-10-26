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
    public String form(){return "redirect:/campaigns/new";}
    @PostMapping("/save")
    public String save(){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,"Luồng đặt đơn cũ đã đóng.");}

    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        model.addAttribute("orders", orders.list(principal.getName()));
        return "order_list";
    }
}
