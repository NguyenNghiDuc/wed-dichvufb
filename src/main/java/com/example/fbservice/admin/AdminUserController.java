package com.example.fbservice.admin;
import com.example.fbservice.web.SearchText;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/admin/users")
public class AdminUserController {
 private final AdminUserService service;
 public AdminUserController(AdminUserService service){this.service=service;}
 @GetMapping
 public String list(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) String q,
   @RequestParam(required=false) String role,@RequestParam(required=false) Boolean enabled,Model model,Principal principal){
  var records=service.page(page,q,role,enabled);
  model.addAttribute("users",records.getContent());model.addAttribute("page",page);
  model.addAttribute("hasNext",records.hasNext());model.addAttribute("hasPrevious",records.hasPrevious());
  model.addAttribute("totalPages",records.getTotalPages());model.addAttribute("totalElements",records.getTotalElements());
  model.addAttribute("ownUsername",principal.getName());model.addAttribute("filterQuery",SearchText.clean(q,120));
  model.addAttribute("filterRole",SearchText.clean(role,16));model.addAttribute("filterEnabled",enabled);
  return "admin_users";
 }
 @PostMapping("/{id}/enabled")
 public String changeEnabled(@PathVariable Long id,@RequestParam boolean enabled,
   @RequestParam(defaultValue="0") int page,@RequestParam(required=false) String q,
   @RequestParam(required=false) String filterRole,@RequestParam(required=false) Boolean filterEnabled,
   Principal principal,Model model,RedirectAttributes redirect){
  service.page(page,q,filterRole,filterEnabled); // Validate navigation fields before any write.
  try{service.setEnabled(principal.getName(),id,enabled);redirect.addFlashAttribute("success","Đã cập nhật trạng thái tài khoản.");return back(page,q,filterRole,filterEnabled,redirect);}
  catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());return list(page,q,filterRole,filterEnabled,model,principal);}
 }
 @PostMapping("/{id}/role")
 public String changeRole(@PathVariable Long id,@RequestParam String role,
   @RequestParam(defaultValue="0") int page,@RequestParam(required=false) String q,
   @RequestParam(required=false) String filterRole,@RequestParam(required=false) Boolean filterEnabled,
   Principal principal,Model model,RedirectAttributes redirect){
  service.page(page,q,filterRole,filterEnabled);
  try{service.setRole(principal.getName(),id,role);redirect.addFlashAttribute("success","Đã cập nhật quyền tài khoản.");return back(page,q,filterRole,filterEnabled,redirect);}
  catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());return list(page,q,filterRole,filterEnabled,model,principal);}
 }
 private String back(int page,String q,String role,Boolean enabled,RedirectAttributes redirect){
  if(page>0)redirect.addAttribute("page",page);
  if(q!=null&&!q.isBlank())redirect.addAttribute("q",q.strip());
  if(role!=null&&!role.isBlank())redirect.addAttribute("role",role.strip());
  if(enabled!=null)redirect.addAttribute("enabled",enabled);
  return "redirect:/admin/users";
 }
 @GetMapping("/audit") public String audit(Model model){model.addAttribute("events",service.recentAudit());return "admin_user_audit";}
}
