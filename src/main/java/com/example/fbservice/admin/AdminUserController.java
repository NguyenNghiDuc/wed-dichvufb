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
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String role,
                       @RequestParam(required = false) Boolean enabled,
                       Model model, Principal principal) {
        Page<User> records = service.page(page, username, role, enabled);
        model.addAttribute("users", records.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", records.getTotalPages());
        model.addAttribute("totalUsers", records.getTotalElements());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("usernameFilter", username == null ? "" : username.trim());
        model.addAttribute("roleFilter", role == null ? "" : role.trim());
        model.addAttribute("enabledFilter", enabled);
        model.addAttribute("ownUsername", principal.getName());
        return "admin_users";
    }

    @PostMapping("/{id}/enabled")
    public String changeEnabled(@PathVariable Long id, @RequestParam boolean enabled,
                                Principal principal, Model model) {
        try {
            service.setEnabled(principal.getName(), id, enabled);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return list(0, null, null, null, model, principal);
        }
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role,
                             Principal principal, Model model) {
        try {
            service.setRole(principal.getName(), id, role);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return list(0, null, null, null, model, principal);
        }
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String actor,
                        @RequestParam(required = false) Long targetId,
                        @RequestParam(required = false) String operation, Model model) {
        Page<AdminAccountAudit> records =
            service.pageAudit(page, actor, targetId, operation);
        model.addAttribute("events", records.getContent());
        model.addAttribute("auditPage", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalAuditPages", records.getTotalPages());
        model.addAttribute("filterActor", actor == null ? "" : actor.trim());
        model.addAttribute("filterTargetId", targetId);
        model.addAttribute("filterOperation", operation == null ? "" : operation.trim());
        return "admin_user_audit";
    }
}
