package com.example.fbservice.promotion;
import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.catalog.ServiceCatalogService;
import com.example.fbservice.config.TrafficLimiter;
import java.security.Principal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class CouponController {
 private final CouponService service;private final ServiceCatalogService catalog;private final TrafficLimiter limiter;private final Clock clock;
 public CouponController(CouponService service,ServiceCatalogService catalog,TrafficLimiter limiter,Clock clock){this.service=service;this.catalog=catalog;this.limiter=limiter;this.clock=clock;}
 @GetMapping("/admin/coupons") public String list(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Boolean enabled,Model model){model.addAttribute("records",service.list(page,enabled));model.addAttribute("enabled",enabled);return "admin_coupons";}
 @GetMapping("/admin/coupons/new") public String form(Model model){if(!model.containsAttribute("input")){var now=LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);model.addAttribute("input",new CouponService.Definition("",CouponType.FIXED,10000,0,0,100,1,now,now.plusDays(7)));}model.addAttribute("types",CouponType.values());return "coupon_form";}
 @PostMapping("/admin/coupons") public String create(@ModelAttribute("input") CouponService.Definition input,BindingResult errors,Principal principal,Model model){
  limiter.check("coupon-create:"+principal.getName(),20,Duration.ofHours(1));
  if(errors.hasErrors()){model.addAttribute("error","Kiểm tra định dạng số và thời gian UTC.");return form(model);}
  try{return "redirect:/admin/coupons/"+service.create(principal.getName(),input).getId();}
  catch(IllegalArgumentException|DataIntegrityViolationException e){model.addAttribute("error",e instanceof DataIntegrityViolationException?"Mã đã tồn tại hoặc dữ liệu không hợp lệ.":e.getMessage());return form(model);}
 }
 @GetMapping("/admin/coupons/{id}") public String detail(@PathVariable Long id,@RequestParam(defaultValue="0") int page,Model model){model.addAttribute("coupon",service.detail(id));model.addAttribute("records",service.uses(id,page));model.addAttribute("events",service.audit(id));return "coupon_detail";}
 @PostMapping("/admin/coupons/{id}/enabled") public String enabled(@PathVariable Long id,@RequestParam boolean expected,@RequestParam boolean enabled,Principal principal,RedirectAttributes redirect){try{service.setEnabled(principal.getName(),id,expected,enabled);redirect.addFlashAttribute("success","Đã cập nhật trạng thái mã. Đơn đã tạo giữ nguyên báo giá.");}catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());}return "redirect:/admin/coupons/"+id;}
 @GetMapping("/campaigns/quote") @ResponseBody
 public ResponseEntity<?> quote(@RequestParam CampaignServiceType serviceType,@RequestParam(required=false) String couponCode,Principal principal){
  limiter.check("coupon-preview:"+principal.getName(),60,Duration.ofMinutes(10));
  try{return ResponseEntity.ok(service.preview(principal.getName(),couponCode,catalog.quote(serviceType)));}
  catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
 }
}
