package com.example.fbservice.security;

import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.config.TrafficLimiter;
import jakarta.servlet.http.*;
import java.security.Principal;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountSecurityController {
 private final UserRepository users;
 private final AccountSessionService sessions;
 private final TrafficLimiter limiter;
 public AccountSecurityController(UserRepository users,AccountSessionService sessions,TrafficLimiter limiter){
  this.users=users;this.sessions=sessions;this.limiter=limiter;
 }
 @GetMapping("/profile/security")
 public String mine(@RequestParam(defaultValue="0") int page,Principal principal,HttpServletRequest request,Model model){
  var account=users.findByUsername(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  var current=request.getSession(false);
  model.addAttribute("sessions",sessions.list(account,current==null?null:current.getId()));
  model.addAttribute("targetUsername",account.getUsername());model.addAttribute("adminView",false);
  history(account.getId(),page,model);return "account_security";
 }
 @GetMapping("/admin/users/{id}/security")
 public String admin(@PathVariable Long id,@RequestParam(defaultValue="0") int page,Model model){
  var account=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  model.addAttribute("sessions",sessions.list(account,null));model.addAttribute("targetUsername",account.getUsername());
  model.addAttribute("adminView",true);model.addAttribute("targetId",id);history(id,page,model);return "account_security";
 }
 @PostMapping("/profile/security/sessions/{id}/revoke")
 public String revoke(@PathVariable Long id,@RequestParam String currentPassword,Principal principal,
      HttpServletRequest request,HttpServletResponse response,Model model,RedirectAttributes redirect){
  limiter.check("session-revoke:"+principal.getName(),5,Duration.ofMinutes(15));
  try{
   boolean current=sessions.revoke(principal.getName(),id,currentPassword,request);
   if(current){
    new SecurityContextLogoutHandler().logout(request,response,SecurityContextHolder.getContext().getAuthentication());
    return "redirect:/login?sessionClosed";
   }
   redirect.addFlashAttribute("success","Đã thu hồi phiên. Thiết bị đó bị chặn ở lần truy cập tiếp theo.");
   return "redirect:/profile/security";
  }catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());return mine(0,principal,request,model);}
 }
 private void history(Long userId,int page,Model model){
  var records=sessions.history(userId,page);model.addAttribute("events",records.getContent());
  model.addAttribute("page",page);model.addAttribute("totalPages",records.getTotalPages());
  model.addAttribute("hasPrevious",records.hasPrevious());model.addAttribute("hasNext",records.hasNext());
 }
}
