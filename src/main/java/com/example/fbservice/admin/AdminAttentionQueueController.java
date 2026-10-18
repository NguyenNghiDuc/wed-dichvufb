package com.example.fbservice.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAttentionQueueController {
    private final AdminAttentionQueueService service;

    public AdminAttentionQueueController(AdminAttentionQueueService service) {
        this.service = service;
    }

    @GetMapping("/admin/attention")
    public String list(@RequestParam(defaultValue = "OVERDUE_PENDING")
                           AdminAttentionQueueService.Queue queue,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        AdminAttentionQueueService.Result result = service.find(queue, page);
        model.addAttribute("orders", result.rows());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("pageNumber", page);
        model.addAttribute("queue", queue);
        model.addAttribute("queues", AdminAttentionQueueService.Queue.values());
        return "admin_attention";
    }
}
