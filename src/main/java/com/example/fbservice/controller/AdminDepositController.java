package com.example.fbservice.controller;

import com.example.fbservice.service.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/admin/deposits")
public class AdminDepositController {
    private final DepositService deposits;
    public AdminDepositController(DepositService deposits){this.deposits=deposits;}
    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) String status,
                       @RequestParam(required=false) String owner,Model model){
        String username=owner==null||owner.isBlank()?null:owner.trim();
        if(username!=null&&username.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        model.addAttribute("records",deposits.page(username,status,page));
        model.addAttribute("selectedStatus",status);model.addAttribute("owner",username);
        return "admin_deposits";
    }
}
