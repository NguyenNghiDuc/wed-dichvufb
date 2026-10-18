package com.example.fbservice.web;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.notification.AccountNotificationRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice(annotations=Controller.class)
public class NavigationModel {
 private final UserRepository users;private final AccountNotificationRepository notifications;
 public NavigationModel(UserRepository users,AccountNotificationRepository notifications){this.users=users;this.notifications=notifications;}
 @ModelAttribute
 public void navigation(Authentication auth,Model model){
  boolean signed=auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken);
  boolean admin=signed&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
  boolean staff=admin||(signed&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_STAFF")));
  model.addAttribute("navSignedIn",signed);model.addAttribute("navAdmin",admin);model.addAttribute("navStaff",staff);
  model.addAttribute("navUnread",signed?notifications.countByOwnerUsernameAndReadAtIsNull(auth.getName()):0L);
  String name=signed?users.findByUsername(auth.getName()).map(u->u.getDisplayName()==null?u.getUsername():u.getDisplayName()).orElse(auth.getName()):"";
  model.addAttribute("navDisplayName",name);
 }
}
