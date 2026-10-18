package com.example.fbservice.admin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Export exactly one bounded Admin priority queue page, with no customer contact fields. */
@RestController
public class AdminAttentionQueueCsvController {
    private final AdminAttentionQueueService queues;
    public AdminAttentionQueueCsvController(AdminAttentionQueueService queues) {
        this.queues=queues;
    }

    @GetMapping(value="/admin/attention/export.csv", produces="text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue="OVERDUE_PENDING") AdminAttentionQueueService.Queue queue,
            @RequestParam(defaultValue="0") int page) {
        var data=queues.find(queue,page);
        StringBuilder csv=new StringBuilder("\uFEFF");
        csv.append("campaign_id,status,service_type,created_at,updated_at,assignee\r\n");
        for (var order:data.rows()) {
            csv.append(order.getId()).append(',')
                .append(AdminActivityCsvController.cell(order.getStatus().name())).append(',')
                .append(AdminActivityCsvController.cell(order.getServiceType().name())).append(',')
                .append(AdminActivityCsvController.cell(order.getCreatedAt().toString())).append(',')
                .append(AdminActivityCsvController.cell(order.getUpdatedAt().toString())).append(',')
                .append(AdminActivityCsvController.cell(order.getAssigneeUsername()))
                .append("\r\n");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"growlab-admin-attention-page.csv\"")
            .header(HttpHeaders.CACHE_CONTROL,"no-store")
            .header("X-Content-Type-Options","nosniff")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
