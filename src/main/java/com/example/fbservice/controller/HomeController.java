package com.example.fbservice.controller;
import com.example.fbservice.catalog.ServiceCatalogService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class HomeController {
 private final ServiceCatalogService catalog;
 public HomeController(ServiceCatalogService catalog){this.catalog=catalog;}
 @GetMapping("/")
 public String home(Model model,Principal principal){
  model.addAttribute("services",catalog.available());model.addAttribute("signedIn",principal!=null);return "home";
 }
}
