package com.example.fbservice.controller;
import com.example.fbservice.tiktok.TiktokPageModel;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class HomeController {
    private final TiktokPageModel page;
    public HomeController(TiktokPageModel page){this.page=page;}
    @GetMapping("/")
    public String home(Model model,Principal principal){page.populate(model,principal);return "home";}
}
