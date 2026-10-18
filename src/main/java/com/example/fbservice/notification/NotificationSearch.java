package com.example.fbservice.notification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Owner-first, parameterized, bounded notification content search. */
@Service
public class NotificationSearch {
    private final AccountNotificationRepository notifications;
    public NotificationSearch(AccountNotificationRepository notifications) {
        this.notifications=notifications;
    }

    public String validate(String value) {
        if (value==null) return "";
        String result=value.strip();
        if (result.codePointCount(0,result.length())>80 ||
                result.codePoints().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid search query");
        return result;
    }

    public Page<AccountNotification> search(String owner, boolean unreadOnly,
                                            LocalDateTime since, String term, Pageable page) {
        // ESCAPE '!' ensures a customer's %, _, and ! are literal characters,
        // not wildcard operators. Criteria parameters prevent SQL injection.
        String pattern="%"+term.toLowerCase(java.util.Locale.ROOT)
            .replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        Specification<AccountNotification> scope=(root,query,cb)->{
            List<jakarta.persistence.criteria.Predicate> clauses=new ArrayList<>();
            clauses.add(cb.equal(root.get("ownerUsername"),owner));
            if(unreadOnly) clauses.add(cb.isNull(root.get("readAt")));
            if(since!=null) clauses.add(cb.greaterThanOrEqualTo(root.get("createdAt"),since));
            clauses.add(cb.or(
                cb.like(cb.lower(root.get("title")),pattern,'!'),
                cb.like(cb.lower(root.get("message")),pattern,'!')));
            return cb.and(clauses.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return notifications.findAll(scope,page);
    }
}
