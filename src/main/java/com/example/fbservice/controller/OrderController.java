package com.example.fbservice.controller;

import com.example.fbservice.model.Order;
import com.example.fbservice.service.OrderService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) Long orderId,
                       Model model, Principal principal) {
        Page<Order> records;
        try {
            records = orders.pageMine(principal.getName(), page, 20, orderId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        }
        model.addAttribute("orders", records.getContent());
        model.addAttribute("orderId", orderId);
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalPages", records.getTotalPages());
        return "order_list";
    }
}
