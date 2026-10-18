package com.example.fbservice.admin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bounded Admin activity CSV for internal review, not a certified audit archive. */
@RestController
public class AdminActivityCsvController {
    private final AdminActivityService activities;

    public AdminActivityCsvController(AdminActivityService activities) {
        this.activities = activities;
    }

    @GetMapping(value = "/admin/activity/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> download(@RequestParam(defaultValue = "ALL")
                                             AdminActivityService.Area area,
                                             @RequestParam(required = false) String actor) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("timestamp,area,actor,target,details\r\n");
        for (AdminActivityService.Entry e : activities.recent(area, actor)) {
            csv.append(cell(e.createdAt().toString())).append(',')
               .append(cell(e.area().name())).append(',')
               .append(cell(e.actor())).append(',')
               .append(cell(e.target())).append(',')
               .append(cell(e.details())).append("\r\n");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-activity.csv\"")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Formula escapes apply after leading spaces/control characters are skipped. */
    static String cell(String input) {
        String value = input == null ? "" : input;
        int index = 0;
        while (index < value.length() &&
                (Character.isWhitespace(value.charAt(index)) ||
                 Character.isSpaceChar(value.charAt(index)) ||
                 value.charAt(index) == '\uFEFF')) {
            index++;
        }
        if (index < value.length() && "=+-@".indexOf(value.charAt(index)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
