package com.example.fbservice.controller;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.service.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/deposit")
public class DepositController {
  private final DepositService depositService;

  public DepositController(DepositService depositService) {
    this.depositService = depositService;
  }

  @GetMapping("/new")
  public String form(Model model) {
    model.addAttribute("deposit", new DepositRequest());
    return "deposit_form";
  }

  @PostMapping("/create")
  public String create(@ModelAttribute("deposit") DepositRequest deposit) {
    depositService.save(deposit);
    return "redirect:/deposit/list";
  }

  @GetMapping("/list")
  public String list(Model model) {
    model.addAttribute("deposits", depositService.findAll());
    return "deposit_list";
  }
}
