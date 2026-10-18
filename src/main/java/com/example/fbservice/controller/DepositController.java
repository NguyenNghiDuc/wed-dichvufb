package com.example.fbservice.controller;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.service.DepositService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/deposit")
public class DepositController {
    private final DepositService deposits;

    public DepositController(DepositService deposits) {
        this.deposits = deposits;
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("deposit", new DepositRequest());
        return "deposit_form";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute DepositRequest input, Principal principal, Model model) {
        try {
            deposits.save(input, principal.getName());
            return "redirect:/deposit/list";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "deposit_form";
        }
    }

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "0") int page,
                       Model model, Principal principal) {
        if (page < 0 || page > DepositService.MAX_HISTORY_PAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        }
        var history = deposits.history(principal.getName(), page);
        model.addAttribute("deposits", history.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("previousPage", page - 1);
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("hasPrevious", history.hasPrevious());
        model.addAttribute("hasNext", history.hasNext());
        model.addAttribute("totalElements", history.getTotalElements());
        return "deposit_list";
    }
}
