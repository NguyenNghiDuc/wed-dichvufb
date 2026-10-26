package com.example.fbservice.support;

import com.example.fbservice.config.TrafficLimiter;
import com.example.fbservice.notification.*;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import com.example.fbservice.web.SearchText;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportTicketService {
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final AccountNotificationRepository notices;
    private final TrafficLimiter limiter;

    public SupportTicketService(SupportTicketRepository tickets, SupportMessageRepository messages,
                                AccountNotificationRepository notices, TrafficLimiter limiter) {
        this.tickets = tickets;
        this.messages = messages;
        this.notices = notices;
        this.limiter = limiter;
    }

    @Transactional
    public SupportTicket create(String user, String subject, String description) {
        validate(subject, 4, 120);
        validate(description, 10, 2000);
        limiter.check("support:create:" + user, 5, Duration.ofHours(1));
        SupportTicket ticket = tickets.save(new SupportTicket(user, subject.strip(), description.strip()));
        notices.save(new AccountNotification(user, "Đã nhận hỗ trợ #" + ticket.getId(),
                "Chúng tôi đã tiếp nhận yêu cầu hỗ trợ của bạn."));
        return ticket;
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> listMine(String user, int page) {
        return searchMine(user,page,null,null,null);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> listForAdmin(int page) {
        return searchForAdmin(page,null,null,null,null);
    }

    @Transactional(readOnly=true)
    public Page<SupportTicket> searchMine(String user,int page,String query,Long id,TicketStatus status){
        if(user==null||user.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return search(user,page,query,id,status,null);
    }

    @Transactional(readOnly=true)
    public Page<SupportTicket> searchForAdmin(int page,String query,Long id,TicketStatus status,String owner){
        return search(null,page,query,id,status,owner);
    }

    private Page<SupportTicket> search(String user,int page,String query,Long id,TicketStatus status,String owner){
        var pageable=paging(page);
        String q=SearchText.clean(query,120),ownerFilter=SearchText.clean(owner,100);
        if(id!=null&&id<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mã yêu cầu không hợp lệ.");
        Specification<SupportTicket> filter=(root,request,builder)->{
            var conditions=new ArrayList<Predicate>();
            if(user!=null)conditions.add(builder.equal(root.get("ownerUsername"),user));
            else if(!ownerFilter.isEmpty())conditions.add(builder.equal(root.get("ownerUsername"),ownerFilter));
            if(id!=null)conditions.add(builder.equal(root.get("id"),id));
            if(status!=null)conditions.add(builder.equal(root.get("status"),status));
            if(!q.isEmpty())conditions.add(builder.like(builder.lower(root.get("subject")),SearchText.contains(q),'\\'));
            return builder.and(conditions.toArray(new Predicate[0]));
        };
        return tickets.findAll(filter,pageable);
    }

    @Transactional(readOnly = true)
    public SupportTicket detailForOwner(String user, Long id) {
        return tickets.findByIdAndOwnerUsername(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public SupportTicket detailForAdmin(Long id) {
        return tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<SupportMessage> messagesForOwner(String user, Long id) {
        detailForOwner(user, id);
        return messages.findByTicketIdOrderByCreatedAtAsc(id);
    }

    @Transactional(readOnly = true)
    public List<SupportMessage> messagesForAdmin(Long id) {
        detailForAdmin(id);
        return messages.findByTicketIdOrderByCreatedAtAsc(id);
    }

    @Transactional
    public void replyAsOwner(String user, Long id, String message) {
        validate(message, 2, 2000);
        SupportTicket ticket = tickets.lockForOwner(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.customerReplied();
        messages.save(new SupportMessage(id, user, message.strip()));
    }

    @Transactional
    public void replyAsAdmin(String admin, Long id, String message) {
        validate(message, 2, 2000);
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.staffReplied();
        messages.save(new SupportMessage(id, admin, message.strip()));
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
            "Hỗ trợ #" + id + " đã phản hồi", "Nhân viên đã phản hồi yêu cầu. Đăng nhập để xem chi tiết."));
    }

    @Transactional
    public void closeAsAdmin(String admin, Long id) {
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.close();
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
            "Hỗ trợ #" + id + " đã đóng", "Yêu cầu đã được đóng bởi nhân viên " + admin + "."));
    }

    @Transactional
    public void closeAsOwner(String user, Long id) {
        SupportTicket ticket = tickets.lockForOwner(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ticket.getStatus() == TicketStatus.CLOSED) return;
        ticket.close();
        notices.save(new AccountNotification(user, "Đã đóng hỗ trợ #" + id,
            "Bạn đã xác nhận đóng yêu cầu hỗ trợ."));
    }

    @Transactional
    public void reopenAsOwner(String user,Long id,String reason) {
        SupportTicket ticket=tickets.lockForOwner(id,user)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        reopen(ticket,user,reason);
    }

    @Transactional
    public void reopenAsAdmin(String admin,Long id,String reason) {
        SupportTicket ticket=tickets.lockForAdmin(id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        reopen(ticket,admin,reason);
    }

    private void reopen(SupportTicket ticket,String actor,String reason) {
        validate(reason,10,1900);
        if(ticket.getStatus()!=TicketStatus.CLOSED)throw new IllegalArgumentException("Chỉ mở lại yêu cầu đã đóng.");
        limiter.check("support:reopen:"+actor,5,Duration.ofHours(1));
        ticket.reopen();
        messages.save(new SupportMessage(ticket.getId(),actor,"[Mở lại yêu cầu] "+reason.strip()));
        notices.save(new AccountNotification(ticket.getOwnerUsername(),"Hỗ trợ #"+ticket.getId()+" đã mở lại",
            "Yêu cầu đã được mở lại. Đăng nhập để xem lịch sử trao đổi và gửi phản hồi."));
    }

    private Pageable paging(int page) {
        if (page < 0 || page > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang không hợp lệ.");
        return PageRequest.of(page, 20, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
    }

    private void validate(String value, int min, int max) {
        if (value == null || value.strip().length() < min || value.length() > max)
            throw new IllegalArgumentException("Nội dung cần từ " + min + " đến " + max + " ký tự.");
    }
}
