package com.example.fbservice.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminStaffDirectoryController {
    private final AdminStaffDirectoryService service;

    public AdminStaffDirectoryController(AdminStaffDirectoryService service) {
        this.service = service;
    }

    @GetMapping("/admin/staff")
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="ALL")
                           AdminStaffDirectoryService.RoleFilter role,
                       @RequestParam(required=false) String username,
                       Model model) {
        var result = service.page(page, role, username);
        model.addAttribute("staff", result.rows());
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("roleFilter", role);
        model.addAttribute("roles", AdminStaffDirectoryService.RoleFilter.values());
        model.addAttribute("usernameFilter", username == null ? "" : username.trim());
        return "admin_staff_directory";
    }
}
