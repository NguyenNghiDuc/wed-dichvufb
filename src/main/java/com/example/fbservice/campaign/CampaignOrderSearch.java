package com.example.fbservice.campaign;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;

/** The owner constraint is mandatory; wildcard input is always treated literally. */
public final class CampaignOrderSearch {
    private CampaignOrderSearch() {}

    public static Specification<CampaignOrder> ownedBy(String owner,CampaignStatus status,
            SocialPlatform platform,String keyword) {
        if(owner==null||owner.isBlank())throw new IllegalArgumentException("Owner required");
        String q=keyword==null?"":keyword.strip();
        if(q.length()>100)throw new IllegalArgumentException("Search too long");
        return (root,query,cb)->{
            List<Predicate> predicates=new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerUsername"),owner));
            if(status!=null)predicates.add(cb.equal(root.get("status"),status));
            if(platform!=null)predicates.add(cb.equal(root.get("platform"),platform));
            if(!q.isEmpty()){
                String text=q.toLowerCase(Locale.ROOT)
                    .replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
                String pattern="%"+text+"%";
                Predicate match=cb.or(
                    cb.like(cb.lower(root.get("brief")),pattern,'\\'),
                    cb.like(cb.lower(root.get("targetUrl")),pattern,'\\'));
                if(q.matches("[0-9]{1,18}")){
                    match=cb.or(match,cb.equal(root.get("id"),Long.parseLong(q)));
                }
                predicates.add(match);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
