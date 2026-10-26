package com.example.fbservice.tiktok;
import com.example.fbservice.campaign.CampaignStatus;
import java.security.Principal;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/admin/tiktok")
public class TiktokAdminController {
    public record ServerDraft(String name,String country,String description,Long price,Integer minimum,Integer maximum,boolean enabled) {}
    private final TiktokOrderingService ordering;
    public TiktokAdminController(TiktokOrderingService ordering){this.ordering=ordering;}
    @GetMapping("/servers")
    public String servers(Model model){
        model.addAttribute("admin",true);
        if(!model.containsAttribute("draft"))model.addAttribute("draft",new ServerDraft("","","",null,null,null,false));
        model.addAttribute("servers",ordering.allServers());
        model.addAttribute("countries",Arrays.stream(Locale.getISOCountries()).map(code->new TiktokOrderingService.Country(code,TiktokOrderingService.countryName(code)))
            .sorted(Comparator.comparing(TiktokOrderingService.Country::name)).toList());
        return "tiktok_servers";
    }
    @PostMapping("/servers")
    public String save(@RequestParam(required=false) Long id,@RequestParam String name,@RequestParam String country,
        @RequestParam(defaultValue="") String description,@RequestParam long price,@RequestParam int minimum,@RequestParam int maximum,
        @RequestParam(defaultValue="false") boolean enabled,@RequestParam(defaultValue="0") long version,Model model,RedirectAttributes redirect){
        try{ordering.saveServer(id,name,country,description,price,minimum,maximum,enabled,version);redirect.addFlashAttribute("success","Đã lưu server. Đơn cũ giữ nguyên báo giá.");return "redirect:/admin/tiktok/servers";}
        catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());if(id==null)model.addAttribute("draft",new ServerDraft(name,country,description,price,minimum,maximum,enabled));return servers(model);}
    }
    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Long orderId,
        @RequestParam(required=false) CampaignStatus status,Model model){
        model.addAttribute("records",ordering.page(null,orderId,status,page));model.addAttribute("statuses",CampaignStatus.values());
        model.addAttribute("orderId",orderId);model.addAttribute("selectedStatus",status);model.addAttribute("admin",true);return "tiktok_orders";
    }
    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id,@RequestParam(defaultValue="0") int page,Model model){
        var order=ordering.adminOrder(id);model.addAttribute("order",order);model.addAttribute("events",ordering.history(id,page));
        List<CampaignStatus> next=switch(order.getStatus()){
            case PENDING_REVIEW->List.of(CampaignStatus.APPROVED,CampaignStatus.REJECTED);
            case APPROVED->List.of(CampaignStatus.IN_PROGRESS,CampaignStatus.REJECTED);
            case IN_PROGRESS->List.of(CampaignStatus.COMPLETED);
            default->List.of();
        };
        model.addAttribute("nextStatuses",next);model.addAttribute("admin",true);return "tiktok_order_detail";
    }
    @PostMapping("/orders/{id}/status")
    public String status(@PathVariable Long id,@RequestParam CampaignStatus status,Principal principal,RedirectAttributes redirect){
        try{ordering.updateStatus(principal.getName(),id,status);redirect.addFlashAttribute("success","Đã cập nhật trạng thái xử lý.");}
        catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());}
        return "redirect:/admin/tiktok/orders/"+id;
    }
}
