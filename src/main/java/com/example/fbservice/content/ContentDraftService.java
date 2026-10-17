package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContentDraftService {
    private static final int PAGE_SIZE = 20;
    private final ContentDraftRepository drafts;
    public ContentDraftService(ContentDraftRepository drafts){this.drafts=drafts;}

    @Transactional
    public ContentDraft save(String owner,SocialPlatform platform,String title,String body,LocalDateTime when){
        validate(owner,platform,title,body,when);
        return drafts.save(new ContentDraft(owner,platform,title.strip(),body.strip(),when));
    }

    @Transactional(readOnly=true)
    public List<ContentDraft> mine(String owner){return drafts.findByOwnerUsernameOrderByCreatedAtDesc(owner);}

    @Transactional(readOnly=true)
    public Page<ContentDraft> pageMine(String owner,int page) {
        if(owner==null||owner.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if(page<0||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");
        return drafts.findByOwnerUsername(owner,PageRequest.of(page,PAGE_SIZE,
            Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
    }

    /**
     * Always apply the authenticated owner restriction inside the database query.
     * Escape LIKE metacharacters so '%' and '_' are literal user search text.
     */
    @Transactional(readOnly=true)
    public Page<ContentDraft> searchMine(String owner, int page, SocialPlatform platform,
                                         DraftStatus status, String search) {
        return searchMine(owner,page,platform,status,search,null,null);
    }

    @Transactional(readOnly=true)
    public Page<ContentDraft> searchMine(String owner, int page, SocialPlatform platform,
                                         DraftStatus status, String search,
                                         LocalDateTime fromUtc, LocalDateTime toUtc) {
        if(owner==null||owner.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if(fromUtc!=null&&toUtc!=null
            && (fromUtc.isAfter(toUtc)||fromUtc.isBefore(toUtc.minusYears(1))))
            throw new IllegalArgumentException("Khoảng lịch UTC không hợp lệ hoặc vượt quá một năm.");
        if(page<0||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");
        String q=search==null?"":search.strip();
        if(q.length()>100 || q.chars().anyMatch(ch -> Character.isISOControl(ch)))
            throw new IllegalArgumentException("Từ khóa phải tối đa 100 ký tự, không có ký tự điều khiển.");
        String pattern="%"+escapeLike(q.toLowerCase(Locale.ROOT))+"%";
        Specification<ContentDraft> scope=(root,query,cb)->{
            List<Predicate> filters=new ArrayList<>();
            filters.add(cb.equal(root.get("ownerUsername"),owner));
            if(platform!=null)filters.add(cb.equal(root.get("platform"),platform));
            if(status!=null)filters.add(cb.equal(root.get("status"),status));
            if(fromUtc!=null)filters.add(cb.greaterThanOrEqualTo(root.get("scheduledAtUtc"),fromUtc));
            if(toUtc!=null)filters.add(cb.lessThanOrEqualTo(root.get("scheduledAtUtc"),toUtc));
            if(!q.isBlank())filters.add(cb.or(
                cb.like(cb.lower(root.get("title")),pattern,'\\'),
                cb.like(cb.lower(root.get("body")),pattern,'\\')));
            return cb.and(filters.toArray(new Predicate[0]));
        };
        return drafts.findAll(scope,PageRequest.of(page,PAGE_SIZE,
            Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
    }

    private static String escapeLike(String value) {
        return value.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
    }

    @Transactional(readOnly=true)
    public ContentDraft findMineById(String owner,Long id) {
        return drafts.findByIdAndOwnerUsername(id,owner)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public ContentDraft revise(String owner,Long id,SocialPlatform platform,
                               String title,String body,LocalDateTime when) {
        validate(owner,platform,title,body,when);
        // Row lock + ownership in SQL avoids racing another request or exposing foreign drafts.
        ContentDraft draft=lockMine(owner,id);
        draft.revise(platform,title.strip(),body.strip(),when);
        return draft;
    }

    @Transactional
    public void deleteMine(String owner,Long id) {
        drafts.delete(lockMine(owner,id));
    }

    private ContentDraft lockMine(String owner,Long id) {
        return drafts.lockByIdAndOwner(id,owner)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validate(String owner,SocialPlatform platform,String title,
                          String body,LocalDateTime when) {
        if(owner==null||owner.isBlank()||platform==null||title==null||title.isBlank()||
            title.length()>120||body==null||body.isBlank()||body.length()>3000)
            throw new IllegalArgumentException("Nội dung hoặc nền tảng không hợp lệ.");
        if(when!=null) {
            LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
            if(when.isBefore(now.plusMinutes(1))||when.isAfter(now.plusYears(1)))
                throw new IllegalArgumentException("Lịch đăng UTC phải từ 1 phút đến 1 năm trong tương lai.");
        }
    }
}
