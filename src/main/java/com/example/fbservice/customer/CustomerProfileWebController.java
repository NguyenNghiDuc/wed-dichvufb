package com.example.fbservice.customer;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CustomerProfileWebController {
    private final CustomerProfileService profiles;
    public CustomerProfileWebController(CustomerProfileService profiles) { this.profiles = profiles; }

    @GetMapping("/profile/edit")
    public String edit(Model model, Principal principal) {
        model.addAttribute("profile", profiles.view(principal.getName()));
        return "customer_profile_edit";
    }

    @PostMapping("/profile/edit")
    public String update(@RequestParam(required=false) String displayName,
            @RequestParam(required=false) String phone, Principal principal, Model model) {
        try {
            profiles.update(principal.getName(), new CustomerProfileRequest(displayName, phone));
            return "redirect:/profile?updated=true";
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() != HttpStatus.BAD_REQUEST) throw exception;
            model.addAttribute("error", "Tên hiển thị 2–80 ký tự; điện thoại phải hợp lệ.");
            model.addAttribute("profile", new CustomerProfileView(principal.getName(), null,
                displayName == null ? "" : displayName, phone == null ? "" : phone));
            return "customer_profile_edit";
        }
    }
}
