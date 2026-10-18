package com.example.fbservice.review;
import com.example.fbservice.config.TrafficLimiter;
import java.security.Principal;
import java.time.Duration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class ServiceReviewController {
 private final ServiceReviewService reviews;private final TrafficLimiter limiter;
 public ServiceReviewController(ServiceReviewService reviews,TrafficLimiter limiter){this.reviews=reviews;this.limiter=limiter;}
 @GetMapping("/reviews/{kind}/{id}")
 public String form(@PathVariable ReviewKind kind,@PathVariable Long id,Principal principal,Model model){
  model.addAttribute("review",reviews.mine(principal.getName(),kind,id).orElse(null));
  model.addAttribute("kind",kind);model.addAttribute("orderId",id);return "service_review";
 }
 @PostMapping("/reviews/{kind}/{id}")
 public String create(@PathVariable ReviewKind kind,@PathVariable Long id,@RequestParam int rating,@RequestParam String comment,
     Principal principal,Model model,RedirectAttributes redirect){
  limiter.check("review:"+principal.getName(),10,Duration.ofMinutes(15));
  try{reviews.create(principal.getName(),kind,id,rating,comment);redirect.addFlashAttribute("success","Đã gửi đánh giá. Cảm ơn bạn đã phản hồi!");return "redirect:/reviews/"+kind+"/"+id;}
  catch(org.springframework.web.server.ResponseStatusException e){
   if(e.getStatusCode().value()!=409)throw e;
   redirect.addFlashAttribute("error","Đơn này đã có đánh giá. Bạn có thể xem lại bên dưới.");
   return "redirect:/reviews/"+kind+"/"+id;
  }
  catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());model.addAttribute("inputRating",rating);model.addAttribute("inputComment",comment);return form(kind,id,principal,model);}
 }
 @GetMapping("/admin/reviews")
 public String admin(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Integer rating,@RequestParam(required=false) ReviewKind kind,Model model){
  model.addAttribute("records",reviews.admin(page,rating,kind));model.addAttribute("rating",rating);model.addAttribute("kind",kind);return "admin_reviews";
 }
}
