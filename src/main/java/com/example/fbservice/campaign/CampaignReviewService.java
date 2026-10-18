package com.example.fbservice.campaign;

import java.util.Optional;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignReviewService {
    private final CampaignOrderRepository orders;
    private final CampaignReviewRepository reviews;
    public CampaignReviewService(CampaignOrderRepository orders, CampaignReviewRepository reviews) {
        this.orders = orders;
        this.reviews = reviews;
    }

    /** Single grouped SQL read; never includes feedback text or other owners. */
    @Transactional(readOnly = true)
    public CustomerReviewSummary summaryForOwner(String username) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        long[] stars = new long[6];
        long total = 0;
        long weighted = 0;
        for (CampaignReviewRepository.OwnerRatingCount row : reviews.groupedRatingsForOwner(username)) {
            int rating = row.getRating();
            if (rating < 1 || rating > 5)
                throw new IllegalStateException("Unexpected rating outside 1..5");
            stars[rating] = row.getTotal();
            total += row.getTotal();
            weighted += rating * row.getTotal();
        }
        double average = total == 0 ? 0.0 : Math.round(weighted * 100.0 / total) / 100.0;
        return new CustomerReviewSummary(total, average, stars[1], stars[2],
                                          stars[3], stars[4], stars[5]);
    }

    @Transactional(readOnly = true)
    public Optional<CampaignReviewView> findForOwner(String username, Long campaignId) {
        verifyOwner(username, campaignId);
        return reviews.findByCampaignIdAndOwnerUsername(campaignId, username).map(CampaignReviewView::from);
    }

    @Transactional(readOnly = true)
    public Page<CampaignReviewView> pageForOwner(String username, int page, int size) {
        return pageForOwner(username, page, size, null, null);
    }

    /**
     * Customer-only review history. Rating and feedback query are combined
     * inside the bounded SQL page; the owner restriction cannot be omitted.
     */
    @Transactional(readOnly = true)
    public Page<CampaignReviewView> pageForOwner(String username, int page, int size,
                                                  Integer rating, String search) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Phân trang đánh giá không hợp lệ.");
        if (rating != null && (rating < 1 || rating > 5))
            throw new IllegalArgumentException("Số sao cần từ 1 đến 5.");
        String cleaned = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        if (cleaned.length() > 80)
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 80 ký tự.");
        Specification<CampaignReview> scope = (root, query, cb) -> {
            List<Predicate> checks = new ArrayList<>();
            checks.add(cb.equal(root.get("ownerUsername"), username));
            if (rating != null) checks.add(cb.equal(root.get("rating"), rating));
            // LOCATE is a literal substring search: % and _ are never SQL wildcards.
            if (!cleaned.isEmpty()) checks.add(cb.greaterThan(
                cb.locate(cb.lower(root.get("feedback")), cleaned), 0));
            return cb.and(checks.toArray(new Predicate[0]));
        };
        return reviews.findAll(scope, PageRequest.of(page, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
            .map(CampaignReviewView::from);
    }

    @Transactional
    public CampaignReviewView submit(String username, Long campaignId, CampaignReviewInput input) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        // Serializes submissions for the same campaign; DB unique constraint is a second safeguard.
        CampaignOrder order = orders.lockByIdAndOwner(campaignId, username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (order.getStatus() != CampaignStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only completed campaigns can be reviewed");
        if (reviews.findByCampaignIdAndOwnerUsername(campaignId, username).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campaign already reviewed");
        if (input == null || input.rating() == null || input.rating() < 1 || input.rating() > 5 ||
            input.feedback() == null || input.feedback().trim().length() < 10 ||
            input.feedback().length() > 500)
            throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao, phản hồi 10–500 ký tự.");
        CampaignReview review = reviews.save(new CampaignReview(campaignId, username,
            input.rating(), input.feedback().trim()));
        return CampaignReviewView.from(review);
    }

    private void verifyOwner(String username, Long campaignId) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        orders.findByIdAndOwnerUsername(campaignId, username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
