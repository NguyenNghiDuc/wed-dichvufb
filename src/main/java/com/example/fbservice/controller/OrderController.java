package com.example.fbservice.controller;

import com.example.fbservice.model.Order;
import com.example.fbservice.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/order")
public class OrderController {
  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping("/new")
  public String form(Model model) {
    model.addAttribute("order", new Order());
    return "order_form";
  }

  @PostMapping("/save")
  public String save(@ModelAttribute Order order, Model model) {
    orderService.create(order);
    return "redirect:/order/list";
  }

  @GetMapping("/list")
  public String list(Model model) {
    model.addAttribute("orders", orderService.list());
    return "order_list";
  }
}
