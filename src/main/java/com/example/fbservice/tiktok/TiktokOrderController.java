package com.example.fbservice.tiktok;
import com.example.fbservice.campaign.CampaignStatus;
import com.example.fbservice.config.TrafficLimiter;
import java.security.Principal;
import java.time.Duration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/tiktok/orders")
public class TiktokOrderController {
    private final TiktokOrderingService ordering;
    private final TiktokPageModel pageModel;
    private final TrafficLimiter limiter;
    public TiktokOrderController(TiktokOrderingService ordering,TiktokPageModel pageModel,TrafficLimiter limiter){this.ordering=ordering;this.pageModel=pageModel;this.limiter=limiter;}
    @GetMapping("/new")
    public String prepare(@RequestParam(defaultValue="") String targetUrl,@RequestParam(required=false) String country,
        @RequestParam(required=false) Long serverId,@RequestParam(defaultValue="100") int quantity,
        @RequestParam(defaultValue="") String note,Model model,Principal principal){
        var request=new TiktokOrderingService.Request(targetUrl,country,serverId,quantity,note);
        if(serverId!=null){
            try{var quote=ordering.quote(request);model.addAttribute("serverVersion",quote.server().version());}
            catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());}
        }
        return checkout(request,model,principal);
    }
    private String checkout(TiktokOrderingService.Request request,Model model,Principal principal){
        model.addAttribute("checkout",true);model.addAttribute("targetUrl",request.targetUrl());
        model.addAttribute("selectedCountry",request.country());model.addAttribute("selectedServerId",request.serverId());
        model.addAttribute("quantity",request.quantity());model.addAttribute("note",request.note());
        pageModel.populate(model,principal);
        ordering.available().stream().filter(s->s.id().equals(request.serverId())).findFirst()
            .ifPresent(s->model.addAttribute("serverVersion",s.version()));
        return "home";
    }
    @PostMapping
    public String create(@RequestParam String targetUrl,@RequestParam String country,@RequestParam Long serverId,
        @RequestParam int quantity,@RequestParam(defaultValue="") String note,@RequestParam long serverVersion,
        Principal principal,Model model,RedirectAttributes redirect){
        limiter.check("tiktok-order:"+principal.getName(),20,Duration.ofMinutes(10));
        var request=new TiktokOrderingService.Request(targetUrl,country,serverId,quantity,note);
        try{
            var order=ordering.create(principal.getName(),request,serverVersion);
            redirect.addFlashAttribute("success","Đã nhận đơn #"+order.getId()+". Đang chờ duyệt, chưa phát sinh thanh toán.");
            return "redirect:/tiktok/orders/"+order.getId();
        }catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());return checkout(request,model,principal);}
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
