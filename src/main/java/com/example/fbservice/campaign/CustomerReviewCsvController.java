package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Review history belongs solely to the logged-in customer. */
@RestController
public class CustomerReviewCsvController {
    private final CustomerReviewCsvExportService exporter;

    public CustomerReviewCsvController(CustomerReviewCsvExportService exporter) {
        this.exporter = exporter;
    }

    @GetMapping(value = "/campaigns/reviews/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> download(@RequestParam(required = false) Integer rating,
                                          @RequestParam(required = false) String q,
                                          Principal principal) {
        try {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"my-campaign-reviews.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(exporter.exportMine(principal.getName(), rating, q));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review CSV filters");
        }
    }
}
