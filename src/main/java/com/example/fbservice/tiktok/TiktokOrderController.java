package com.example.fbservice.tiktok;
import com.example.fbservice.campaign.CampaignStatus;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/tiktok/orders")
public class TiktokOrderController {
    private final TiktokOrderingService ordering;
    public TiktokOrderController(TiktokOrderingService ordering){this.ordering=ordering;}
    @GetMapping("/new")
    public String prepare(){return "redirect:/campaigns/new";}
    @PostMapping
    public String create(){
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,"Luồng đặt đơn cũ đã ngừng nhận yêu cầu. Vui lòng chọn gói tư vấn.");
    }
    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Long orderId,
        @RequestParam(required=false) CampaignStatus status,Principal principal,Model model){
        model.addAttribute("records",ordering.page(principal.getName(),orderId,status,page));
        model.addAttribute("orderId",orderId);model.addAttribute("selectedStatus",status);model.addAttribute("statuses",CampaignStatus.values());
        model.addAttribute("admin",false);return "tiktok_orders";
    }
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,@RequestParam(defaultValue="0") int page,Principal principal,Model model){
        model.addAttribute("order",ordering.mine(principal.getName(),id));
        model.addAttribute("events",ordering.history(id,page));model.addAttribute("admin",false);return "tiktok_order_detail";
    }
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,Principal principal,RedirectAttributes redirect){
        try{ordering.cancel(principal.getName(),id);redirect.addFlashAttribute("success","Đã hủy đơn. Không phát sinh thanh toán.");}
        catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());}
        return "redirect:/tiktok/orders/"+id;
    }
}
