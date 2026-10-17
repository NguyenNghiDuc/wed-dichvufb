package com.example.fbservice.admin;

import com.example.fbservice.model.User;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service=service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page, Model model, Principal principal) {
        Page<User> users=service.page(page);
        model.addAttribute("users", users.getContent());
        model.addAttribute("page", page);
        model.addAttribute("hasNext", users.hasNext());
        model.addAttribute("hasPrevious", users.hasPrevious());
        model.addAttribute("ownUsername",principal.getName());
        return "admin_users";
    }

    @PostMapping("/{id}/enabled")
    public String changeEnabled(@PathVariable Long id,@RequestParam boolean enabled,
                                Principal principal,Model model) {
        try{
            service.setEnabled(principal.getName(),id,enabled);
            return "redirect:/admin/users";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return list(0,model,principal);
        }
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id,@RequestParam String role,
                             Principal principal,Model model) {
        try{
            service.setRole(principal.getName(),id,role);
            return "redirect:/admin/users";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return list(0,model,principal);
        }
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("events",service.recentAudit());
        return "admin_user_audit";
    }
}
