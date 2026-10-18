package com.example.fbservice.catalog;

import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.campaign.SocialPlatform;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Customer-facing discovery: server-only active plans and catalog prices. */
@Controller
public class ServiceDiscoveryController {
    public record ServiceCard(String platformName, String platformSlug, String planSlug,
                              String title, long priceVnd, String shortDescription) {}
    private final ServiceCatalogService catalog;

    public ServiceDiscoveryController(ServiceCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/dich-vu")
    public String search(@RequestParam(defaultValue = "") String q,
                         @RequestParam(required = false) SocialPlatform platform,
                         @RequestParam(required = false) Long minPrice,
                         @RequestParam(required = false) Long maxPrice,
                         @RequestParam(defaultValue = "price_asc") String sort,
                         Model model) {
        if (q.length() > 100 || (minPrice != null && (minPrice < 0 || minPrice > 50_000_000))
                || (maxPrice != null && (maxPrice < 0 || maxPrice > 50_000_000))
                || (minPrice != null && maxPrice != null && minPrice > maxPrice)
                || !List.of("price_asc", "price_desc", "name").contains(sort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bộ lọc không hợp lệ.");
        }
        String needle = simplify(q.strip());
        List<ServiceCard> cards = new ArrayList<>();
        for (ServiceOffering offering : catalog.available()) {
            for (SocialPlatform candidate : SocialPlatform.values()) {
                if (platform != null && platform != candidate) continue;
                String title = offering.getTitle() + " " + candidate.getDisplayName();
                if (!needle.isEmpty()
                        && !simplify(title + " " + offering.getCode()).contains(needle)) continue;
                long price = offering.getPriceVnd();
                if (minPrice != null && price < minPrice) continue;
                if (maxPrice != null && price > maxPrice) continue;
                String description = switch (CampaignServiceType.valueOf(offering.getCode())) {
                    case PROFILE_AUDIT -> "Đánh giá hồ sơ và đề xuất cải thiện kênh.";
                    case CONTENT_PLAN -> "Lập kế hoạch nội dung 7 ngày theo mục tiêu.";
                    case AD_STRATEGY -> "Tư vấn quảng cáo theo ngân sách và đối tượng.";
                };
                cards.add(new ServiceCard(candidate.getDisplayName(), slug(candidate.name()),
                        slug(offering.getCode()), title, price, description));
            }
        }
        Comparator<ServiceCard> comparator = switch (sort) {
            case "price_desc" -> Comparator.comparingLong(ServiceCard::priceVnd).reversed()
                    .thenComparing(ServiceCard::title);
            case "name" -> Comparator.comparing(ServiceCard::title, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingLong(ServiceCard::priceVnd).thenComparing(ServiceCard::title);
        };
        cards.sort(comparator);
        model.addAttribute("cards", cards);
        model.addAttribute("count", cards.size());
        model.addAttribute("query", q.strip());
        model.addAttribute("selectedPlatform", platform);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("platforms", SocialPlatform.values());
        return "service_catalog";
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String simplify(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
