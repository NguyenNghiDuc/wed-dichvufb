package com.example.fbservice.admin;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        Model model) {
        model.addAttribute("entries", activities.recent(area, actor, from, to));
        model.addAttribute("actorFilter", actor == null ? "" : actor.trim());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("selectedArea", area);
        model.addAttribute("areas", AdminActivityService.Area.values());
        return "admin_activity";
    }
}
