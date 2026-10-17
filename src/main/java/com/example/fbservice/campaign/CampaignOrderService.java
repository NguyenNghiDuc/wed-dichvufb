package com.example.fbservice.campaign;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignOrderService {
    private final CampaignOrderRepository orders;

    public CampaignOrderService(CampaignOrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public CampaignOrder create(String username, CampaignOrderRequest request) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        validate(request);
        return orders.save(new CampaignOrder(username, request));
    }

    @Transactional(readOnly = true)
    public List<CampaignOrder> findMine(String username) {
        return orders.findByOwnerUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public CampaignOrder findMineById(String username, Long id) {
        return orders.findByIdAndOwnerUsername(id, username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<CampaignOrder> findAllForAdmin() {
        return orders.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public CampaignOrder updateStatusForAdmin(Long id, CampaignStatus status) {
        CampaignOrder order = orders.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        order.changeStatus(status);
        return order;
    }

    private void validate(CampaignOrderRequest request) {
        if (request == null || request.platform() == null || request.serviceType() == null
            || request.targetUrl() == null || request.targetUrl().length() > 512
            || request.brief() == null || request.brief().trim().length() < 10
            || request.brief().length() > 1000) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin chiến dịch.");
        }
        try {
            URI uri = URI.create(request.targetUrl().trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null || uri.getPort() != -1
                || uri.getHost() == null || !request.platform().acceptsHost(uri.getHost())) {
                throw new IllegalArgumentException("Link HTTPS phải thuộc nền tảng đã chọn.");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Link HTTPS phải thuộc nền tảng đã chọn.");
        }
    }
}
