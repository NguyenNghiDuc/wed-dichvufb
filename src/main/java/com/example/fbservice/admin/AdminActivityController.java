package com.example.fbservice.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminActivityController {
    private final AdminActivityService activities;

    public AdminActivityController(AdminActivityService activities) {
        this.activities = activities;
    }

    @GetMapping("/admin/activity")
    public String index(@RequestParam(defaultValue = "ALL") AdminActivityService.Area area,
                        @RequestParam(required = false) String actor,
                        Model model) {
        model.addAttribute("entries", activities.recent(area, actor));
        model.addAttribute("actorFilter", actor == null ? "" : actor.trim());
        model.addAttribute("selectedArea", area);
        model.addAttribute("areas", AdminActivityService.Area.values());
        return "admin_activity";
    }
}
