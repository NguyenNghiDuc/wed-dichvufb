package com.example.fbservice.controller;
import com.example.fbservice.campaign.CampaignOrderService;
import com.example.fbservice.tiktok.TiktokOrderingService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class OrderConfirmationController {
 private final CampaignOrderService campaigns;private final TiktokOrderingService tiktok;
 public OrderConfirmationController(CampaignOrderService campaigns,TiktokOrderingService tiktok){this.campaigns=campaigns;this.tiktok=tiktok;}
 @GetMapping("/campaigns/{id}/success")
 public String campaign(@PathVariable Long id,Principal principal,Model model){
  var o=campaigns.findMineById(principal.getName(),id);
  model.addAttribute("orderId",o.getId());model.addAttribute("kind","campaign");model.addAttribute("serviceTitle",o.getServiceType().getDisplayName());
  model.addAttribute("platform",o.getPlatform().getDisplayName());model.addAttribute("targetUrl",o.getTargetUrl());
  model.addAttribute("original",o.getOriginalPriceVnd());model.addAttribute("discount",o.getDiscountVnd());model.addAttribute("coupon",o.getCouponCode());
  model.addAttribute("price",o.getQuotedPriceVnd());model.addAttribute("status",o.getStatus().getDisplayName());model.addAttribute("createdAt",o.getCreatedAt());return "order_success";
 }
 @GetMapping("/tiktok/orders/{id}/success")
 public String tiktok(@PathVariable Long id,Principal principal,Model model){
  var o=tiktok.mine(principal.getName(),id);
  model.addAttribute("orderId",o.getId());model.addAttribute("kind","tiktok");model.addAttribute("serviceTitle",o.getServerName());
  model.addAttribute("platform","TikTok · "+o.getCountry()+" · "+o.getQuantity()+" followers");model.addAttribute("targetUrl",o.getTargetUrl());
  model.addAttribute("original",o.getTotalPriceVnd());model.addAttribute("discount",0);model.addAttribute("coupon",null);
  model.addAttribute("price",o.getTotalPriceVnd());model.addAttribute("status",o.getStatus().getDisplayName());model.addAttribute("createdAt",o.getCreatedAt());return "order_success";
 }
}
