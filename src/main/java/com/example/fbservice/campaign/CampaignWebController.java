package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/campaigns")
public class CampaignWebController {
    private final CampaignOrderService service;
    private final com.example.fbservice.catalog.ServiceCatalogService catalog;
    public CampaignWebController(CampaignOrderService service,
        com.example.fbservice.catalog.ServiceCatalogService catalog) {
        this.service=service;this.catalog=catalog;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) Long orderId,
                       @RequestParam(required = false) CampaignStatus status,
                       Model model, Principal principal) {
        Page<CampaignOrder> records;
        try {
            records = service.pageMine(principal.getName(), page, 20, orderId, status);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filters or pagination");
        }
        // Aggregate across every page, including orders hidden by the current filter.
        model.addAttribute("summary", service.summaryForOwner(principal.getName()));
        model.addAttribute("orders", records.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalPages", records.getTotalPages());
        model.addAttribute("orderId", orderId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", CampaignStatus.values());
        return "campaign_list";
    }

    @GetMapping("/new")
    public String selectedForm(@RequestParam(required = false) SocialPlatform platform,
                               @RequestParam(required = false) CampaignServiceType serviceType,
                               @RequestParam(required = false) String targetUrl,
                               @RequestParam(required = false) String brief,
                               Model model) {
        model.addAttribute("selectedPlatform", platform);
        model.addAttribute("selectedService", serviceType == null ? null : serviceType.name());
        model.addAttribute("targetUrl", targetUrl);
        model.addAttribute("brief", brief);
        return form(model);
    }

    public String form(Model model) {
        model.addAttribute("platforms", SocialPlatform.values());
        model.addAttribute("plans", catalog.available());
        return "campaign_form";
    }

    @PostMapping
    public String create(@RequestParam SocialPlatform platform, @RequestParam CampaignServiceType serviceType,
                         @RequestParam String targetUrl, @RequestParam String brief,
                         @RequestParam(name = "receipt", defaultValue = "false") boolean receipt,
                         Principal principal, Model model, RedirectAttributes redirect) {
        try {
            CampaignOrder created = service.create(principal.getName(), new CampaignOrderRequest(platform, serviceType, targetUrl, brief));
            redirect.addFlashAttribute("success", "Đã nhận yêu cầu #" + created.getId() + ". Chiến dịch đang chờ duyệt.");
            redirect.addFlashAttribute("createdOrderId", created.getId());
            return receipt ? "redirect:/campaigns/" + created.getId() + "/created" : "redirect:/campaigns";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return selectedForm(platform, serviceType, targetUrl, brief, model);
        }
    }

    /**
     * Ownership-checked confirmation for newly created consultation requests.
     * Price and status always come from the stored order, never URL parameters.
     */
    @GetMapping("/{id}/created")
    public String confirmation(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("order", service.findMineById(principal.getName(), id));
        return "campaign_created";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            service.cancelForOwner(principal.getName(), id);
            redirect.addFlashAttribute("success", "Đã hủy yêu cầu #" + id + ". Không phát sinh thanh toán.");
            return "redirect:/campaigns";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return list(0, null, null, model, principal);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                          Principal principal, Model model) {
        Page<CampaignHistoryEntry> history;
        try {
            history = service.pageHistoryForOwner(principal.getName(), id, page, 20);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid history page");
        }
        model.addAttribute("order", service.findMineById(principal.getName(), id));
        model.addAttribute("events", history.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("totalPages", history.getTotalPages());
        model.addAttribute("hasPrevious", history.hasPrevious());
        model.addAttribute("hasNext", history.hasNext());
        return "campaign_details";
    }
}
