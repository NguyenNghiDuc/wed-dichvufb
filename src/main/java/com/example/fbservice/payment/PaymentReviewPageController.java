package com.example.fbservice.payment;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Separate admin-only sandbox operations page; no payment actions. */
@Controller
@RequestMapping("/admin/payments")
public class PaymentReviewPageController {
    private final PaymentReconciliationService reconciliation;
    private final PaymentDailySummaryService daily;

    public PaymentReviewPageController(PaymentReconciliationService reconciliation,
            PaymentDailySummaryService daily) {
        this.reconciliation = reconciliation;
        this.daily = daily;
    }

    @GetMapping("/review")
    public String review(@RequestParam(defaultValue = "7") int days,
            Model model, HttpServletResponse response) {
        // Validate bounds before querying reconciliation, so bad inputs cost
        // no global aggregate scan.
        var trend = daily.report(days);
        var summary = reconciliation.summary();
        response.setHeader("Cache-Control", "no-store, max-age=0");
        model.addAttribute("summary", summary);
        model.addAttribute("trend", trend);
        model.addAttribute("days", days);
        return "payment_review";
    }
}
