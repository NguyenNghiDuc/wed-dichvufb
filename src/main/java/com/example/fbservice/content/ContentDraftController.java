package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/content-calendar")
public class ContentDraftController {
    private static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private final ContentDraftService service;
    public ContentDraftController(ContentDraftService service){this.service=service;}

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,Model model,Principal principal){
        Page<ContentDraft> records;
        try{
            records=service.pageMine(principal.getName(),page);
        }catch(IllegalArgumentException ex){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trang không hợp lệ.");
        }
        model.addAttribute("drafts",records.getContent());
        model.addAttribute("pageNumber",page);
        model.addAttribute("hasPrevious",records.hasPrevious());
        model.addAttribute("hasNext",records.hasNext());
        model.addAttribute("totalPages",records.getTotalPages());
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
            return list(0,model,principal);
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id,Principal principal,Model model){
        ContentDraft draft=service.findMineById(principal.getName(),id);
        model.addAttribute("draft",draft);
        model.addAttribute("platforms",SocialPlatform.values());
        model.addAttribute("scheduleValue",draft.getScheduledAtUtc()==null?""
            :draft.getScheduledAtUtc().format(INPUT_DATE));
        return "content_draft_edit";
    }

    @PostMapping("/{id}/edit")
    public String revise(@PathVariable Long id,@RequestParam SocialPlatform platform,
                         @RequestParam String title,@RequestParam String body,
                         @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAtUtc,
                         Principal principal,Model model){
        try{
            service.revise(principal.getName(),id,platform,title,body,scheduledAtUtc);
            return "redirect:/content-calendar";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return edit(id,principal,model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,Principal principal){
        service.deleteMine(principal.getName(),id);
        return "redirect:/content-calendar";
    }
}
