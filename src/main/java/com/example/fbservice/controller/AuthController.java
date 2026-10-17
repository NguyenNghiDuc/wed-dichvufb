package com.example.fbservice.controller;

import com.example.fbservice.model.User;
import com.example.fbservice.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserService users;
    public AuthController(UserService users){this.users=users;}

    @GetMapping("/login")
    public String loginPage(jakarta.servlet.http.HttpServletResponse response) {
        // A fixed, nonsensitive marker helps identify the currently running binary in Codespaces.
        response.setHeader("X-GrowLab-Auth-Revision", "session-recovery-v2");
        response.setHeader("Cache-Control", "no-store");
        return "login";
    }
    @GetMapping("/register")
    public String registerPage(){return "register";}

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,@RequestParam String password,
                               @RequestParam(required=false) String email,Model model){
        String name=username==null?"":username.trim();
        if(!name.matches("[a-zA-Z0-9._-]{3,40}")){
            model.addAttribute("error","Tên tài khoản cần 3–40 ký tự chữ, số, dấu chấm hoặc gạch.");
            return "register";
        }
        if(password==null||password.length()<8||password.length()>72){
            model.addAttribute("error","Mật khẩu cần từ 8 đến 72 ký tự.");
            return "register";
        }
        String cleanEmail=email==null?null:email.trim();
        if(cleanEmail!=null&&!cleanEmail.isBlank()&&
           (cleanEmail.length()>255||!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))){
            model.addAttribute("error","Địa chỉ email không hợp lệ.");
            return "register";
        }
        if(users.existsByUsername(name)){
            model.addAttribute("error","Tên tài khoản đã tồn tại.");
            return "register";
        }
        User user=new User();user.setUsername(name);user.setPassword(password);
        user.setEmail(cleanEmail==null||cleanEmail.isBlank()?null:cleanEmail);
        try{users.saveUser(user);}
        catch(DataIntegrityViolationException ex){
            model.addAttribute("error","Tài khoản không hợp lệ hoặc đã tồn tại.");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
