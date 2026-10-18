package com.example.fbservice.controller;

import com.example.fbservice.model.Order;
import com.example.fbservice.service.OrderService;
import com.example.fbservice.service.LegacyOrderSummaryService;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final LegacyOrderSummaryService summaries;

    public OrderController(OrderService orders, LegacyOrderSummaryService summaries) {
        this.orders = orders;
        this.summaries = summaries;
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
                       @RequestParam(required = false) String service,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model, Principal principal) {
        Page<Order> records;
        try {
            records = orders.pageMine(principal.getName(), page, 20, orderId, service, from, to);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        }
        model.addAttribute("orders", records.getContent());
        model.addAttribute("summary", summaries.forOwner(principal.getName()));
        model.addAttribute("orderId", orderId);
        model.addAttribute("selectedService", service);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalPages", records.getTotalPages());
        return "order_list";
    }
}
