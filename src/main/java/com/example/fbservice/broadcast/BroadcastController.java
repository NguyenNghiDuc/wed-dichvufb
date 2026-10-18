package com.example.fbservice.broadcast;
import com.example.fbservice.config.TrafficLimiter;
import java.security.Principal;
import java.time.Duration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/admin/broadcasts")
public class BroadcastController {
 private final BroadcastService service;private final TrafficLimiter limiter;
 public BroadcastController(BroadcastService service,TrafficLimiter limiter){this.service=service;this.limiter=limiter;}
 @GetMapping public String list(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) BroadcastStatus status,Model model){model.addAttribute("records",service.list(page,status));model.addAttribute("selectedStatus",status);model.addAttribute("statuses",BroadcastStatus.values());return "admin_broadcasts";}
 @GetMapping("/new") public String form(Model model){model.addAttribute("audiences",BroadcastAudience.values());return "broadcast_form";}
 @PostMapping public String create(@RequestParam String title,@RequestParam String message,@RequestParam BroadcastAudience audience,Principal principal,Model model){
  limiter.check("broadcast-draft:"+principal.getName(),10,Duration.ofHours(1));
  try{return "redirect:/admin/broadcasts/"+service.create(principal.getName(),title,message,audience).getId();}
  catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());model.addAttribute("inputTitle",title);model.addAttribute("inputMessage",message);model.addAttribute("inputAudience",audience);return form(model);}
 }
 @GetMapping("/{id}") public String detail(@PathVariable Long id,Model model){var b=service.detail(id);model.addAttribute("broadcast",b);model.addAttribute("events",service.history(id));model.addAttribute("recipientCount",b.getStatus()==BroadcastStatus.DRAFT?service.recipients(b.getAudience()):b.getRecipientCount());return "broadcast_detail";}
 @PostMapping("/{id}/send") public String send(@PathVariable Long id,@RequestParam int confirmedRecipients,Principal principal,RedirectAttributes redirect){
  limiter.check("broadcast-send:"+principal.getName(),5,Duration.ofMinutes(15));
  try{var b=service.send(principal.getName(),id,confirmedRecipients);redirect.addFlashAttribute("success","Đã gửi tới "+b.getRecipientCount()+" tài khoản. Gửi lại cùng mã không tạo thông báo trùng.");}
  catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());}return "redirect:/admin/broadcasts/"+id;
 }
 @PostMapping("/{id}/cancel") public String cancel(@PathVariable Long id,Principal principal,RedirectAttributes redirect){try{service.cancel(principal.getName(),id);redirect.addFlashAttribute("success","Đã hủy bản nháp.");}catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());}return "redirect:/admin/broadcasts/"+id;}
}
