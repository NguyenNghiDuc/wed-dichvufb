package com.example.fbservice.service;

import com.example.fbservice.model.Order;
import com.example.fbservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.data.domain.*;

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

    /** Customer-scoped, bounded replacement for the legacy unlimited web list. */
    public Page<Order> pageMine(String username, int page, int size) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Giới hạn phân trang không hợp lệ.");
        return orders.findByOwnerUsername(username, PageRequest.of(page, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }
}
