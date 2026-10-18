package com.example.fbservice.service;

import com.example.fbservice.model.Order;
import com.example.fbservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository orders;
    public OrderService(OrderRepository orders){this.orders=orders;}

    public Order create(Order input,String username) {
        if(username==null||username.isBlank()||input==null||
            input.getQuantity()<1||input.getQuantity()>10000||
            input.getLinkOrder()==null||input.getLinkOrder().isBlank()||input.getLinkOrder().length()>255||
            (input.getServerName()!=null && input.getServerName().length()>255)||
            (input.getNote()!=null && input.getNote().length()>255)){
            throw new IllegalArgumentException("Thông tin đơn hàng không hợp lệ.");
        }
        Order o=new Order();
        o.setOwnerUsername(username);
        o.setLinkOrder(input.getLinkOrder().trim());
        o.setServerName(input.getServerName());
        o.setQuantity(input.getQuantity());
        o.setNote(input.getNote());
        o.setPricePerUnit(10);
        return orders.save(o);
    }
    public List<Order> list(String username){return orders.findByOwnerUsernameOrderByCreatedAtDesc(username);}

    /** Return the same 404 for a foreign or nonexistent order ID. */
    public Order findMineById(String username, Long id) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (id == null || id <= 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return orders.findByIdAndOwnerUsername(id, username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Customer-scoped, bounded replacement for the legacy unlimited web list. */
    public Page<Order> pageMine(String username, int page, int size) {
        return pageMine(username, page, size, null, null, null, null);
    }

    public Page<Order> pageMine(String username, int page, int size, Long orderId) {
        return pageMine(username, page, size, orderId, null, null, null);
    }

    public Page<Order> pageMine(String username, int page, int size, Long orderId,
                                String service, LocalDate from, LocalDate to) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Giới hạn phân trang không hợp lệ.");
        if (orderId != null && orderId <= 0)
            throw new IllegalArgumentException("Mã đơn phải là số nguyên dương.");
        if ((from != null && (from.getYear() < 2000 || from.getYear() > 2100)) ||
            (to != null && (to.getYear() < 2000 || to.getYear() > 2100)) ||
            (from != null && to != null && from.isAfter(to)))
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        if (service != null && service.length() > 255)
            throw new IllegalArgumentException("Tên dịch vụ quá dài.");
        String exactService = service == null || service.isBlank() ? null : service.trim();
        LocalDateTime lower = from == null ? null : from.atStartOfDay();
        LocalDateTime upper = to == null ? null : to.plusDays(1).atStartOfDay();
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> terms = new ArrayList<>();
            // Account ownership must always be part of the SQL predicate.
            terms.add(cb.equal(root.get("ownerUsername"), username));
            if (orderId != null) terms.add(cb.equal(root.get("id"), orderId));
            if (exactService != null) terms.add(cb.equal(root.get("serverName"), exactService));
            if (lower != null) terms.add(cb.greaterThanOrEqualTo(root.get("createdAt"), lower));
            if (upper != null) terms.add(cb.lessThan(root.get("createdAt"), upper));
            return cb.and(terms.toArray(new Predicate[0]));
        };
        Pageable bounds = PageRequest.of(page, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return orders.findAll(spec, bounds);
    }
}
