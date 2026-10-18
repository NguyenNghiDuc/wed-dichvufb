package com.example.fbservice.admin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** CSV for operational records, not employee scoring or payroll. */
@RestController
public class AdminStaffPerformanceCsvController {
    private final AdminStaffPerformanceService performance;
    public AdminStaffPerformanceCsvController(AdminStaffPerformanceService performance) {
        this.performance=performance;
    }

    @GetMapping(value="/admin/staff/performance.csv", produces="text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export() {
        StringBuilder out=new StringBuilder("\uFEFFactor,started_orders,completed_orders\r\n");
        for (var row:performance.lastThirtyDays()) {
            // Actor usernames derive from historical audit records: still escape formulas.
            out.append(AdminActivityCsvController.cell(row.actor())).append(',')
                .append(row.started()).append(',').append(row.completed()).append("\r\n");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL,"no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"admin-staff-performance.csv\"")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(out.toString().getBytes(StandardCharsets.UTF_8));
    }
}
