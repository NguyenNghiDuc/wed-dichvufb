package com.example.fbservice.campaign;

import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/campaigns")
public class CampaignWebController {
    private final CampaignOrderService service;
    private final CampaignReviewService reviews;
    private final CampaignAttentionService attention;
    private final CampaignProgressService progress;
    private final com.example.fbservice.catalog.ServiceCatalogService catalog;
    public CampaignWebController(CampaignOrderService service,
        com.example.fbservice.catalog.ServiceCatalogService catalog,
        CampaignReviewService reviews, CampaignAttentionService attention, CampaignProgressService progress) {
        this.service=service;this.catalog=catalog;this.reviews=reviews;this.attention=attention;this.progress=progress;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) Long orderId,
                       @RequestParam(required = false) CampaignStatus status,
                       @RequestParam(required = false) CampaignServiceType serviceType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model, Principal principal) {
        Page<CampaignOrder> records;
        try {
            records = service.pageMine(principal.getName(), page, 20, orderId, status, serviceType, from, to);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filters or pagination");
        }
        // Aggregate across every page, including orders hidden by the current filter.
        model.addAttribute("summary", service.summaryForOwner(principal.getName()));
        model.addAttribute("orders", records.getContent());
        // Only current owner's 20 visible records; no additional database queries.
        model.addAttribute("attentionById", records.getContent().stream()
            .collect(java.util.stream.Collectors.toMap(CampaignOrder::getId, attention::inspect)));
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalPages", records.getTotalPages());
        model.addAttribute("orderId", orderId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedServiceType", serviceType);
        model.addAttribute("serviceTypes", CampaignServiceType.values());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("statuses", CampaignStatus.values());
        return "campaign_list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("platforms", SocialPlatform.values());
        model.addAttribute("plans", catalog.available());
        return "campaign_form";
    }

    @PostMapping
    public String create(@RequestParam SocialPlatform platform, @RequestParam CampaignServiceType serviceType,
                         @RequestParam String targetUrl, @RequestParam String brief,
                         Principal principal, Model model) {
        try {
            service.create(principal.getName(), new CampaignOrderRequest(platform, serviceType, targetUrl, brief));
            return "redirect:/campaigns";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("submittedPlatform", platform);
            model.addAttribute("submittedServiceType", serviceType);
            model.addAttribute("submittedTargetUrl", targetUrl);
            model.addAttribute("submittedBrief", brief);
            return form(model);
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            service.cancelForOwner(principal.getName(), id);
            flash.addFlashAttribute("notice", "Đã hủy đơn chưa xử lý.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/campaigns";
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
        // Load the owned order once; reuse it for independent read-only view models.
        CampaignOrder order = service.findMineById(principal.getName(), id);
        model.addAttribute("order", order);
        model.addAttribute("review", reviews.findForOwner(principal.getName(), id).orElse(null));
        model.addAttribute("attention", attention.inspect(order));
        model.addAttribute("progress", progress.view(order));
        model.addAttribute("events", history.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("totalPages", history.getTotalPages());
        model.addAttribute("hasPrevious", history.hasPrevious());
        model.addAttribute("hasNext", history.hasNext());
        return "campaign_details";
    }
}
