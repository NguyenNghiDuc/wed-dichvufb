package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.security.Principal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/content-calendar")
public class ContentDraftController {
    private final ContentDraftService service;
    public ContentDraftController(ContentDraftService service){this.service=service;}
    @GetMapping
    public String list(Model model,Principal principal){
        model.addAttribute("drafts",service.mine(principal.getName()));
        model.addAttribute("platforms",SocialPlatform.values());
        return "content_calendar";
    }
    @PostMapping
    public String create(@RequestParam SocialPlatform platform,@RequestParam String title,
                         @RequestParam String body,
                         @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAtUtc,
                         Principal principal,Model model){
        try{
            service.save(principal.getName(),platform,title,body,scheduledAtUtc);
            return "redirect:/content-calendar";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return list(model,principal);
        }
    }
}
