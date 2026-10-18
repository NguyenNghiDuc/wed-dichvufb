package com.example.fbservice.admin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Export the selected directory page, not the complete employee database.
 * Only public operational columns are included, not contact details or credentials.
 */
@RestController
public class AdminStaffDirectoryCsvController {
    private final AdminStaffDirectoryService directory;

    public AdminStaffDirectoryCsvController(AdminStaffDirectoryService directory) {
        this.directory = directory;
    }

    @GetMapping(value = "/admin/staff/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "ALL") AdminStaffDirectoryService.RoleFilter role,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean enabled) {
        var result = directory.page(page, role, username, enabled);
        StringBuilder out = new StringBuilder("\uFEFF");
        out.append("username,role,enabled,active_orders\r\n");
        for (var person : result.rows()) {
            out.append(AdminActivityCsvController.cell(person.username())).append(',')
                .append(AdminActivityCsvController.cell(person.role())).append(',')
                .append(person.enabled()).append(',')
                .append(person.activeOrders()).append("\r\n");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"growlab-staff-page.csv\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(out.toString().getBytes(StandardCharsets.UTF_8));
    }
}
