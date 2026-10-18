package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
    public List<ContentDraft> mine(String owner){
        requireOwner(owner);
        return drafts.findByOwnerUsernameOrderByCreatedAtDesc(owner);
    }

    @Transactional(readOnly=true)
    public Page<ContentDraft> pageMine(String owner,int page) {
        requireOwner(owner);
        if(page<0||page>10000)throw new IllegalArgumentException("Trang không hợp lệ.");
        return drafts.findByOwnerUsername(owner,PageRequest.of(page,PAGE_SIZE,
            Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
    }

    @Transactional(readOnly=true)
    public ContentDraft findMineById(String owner,Long id) {
        requireOwner(owner);
        requireId(id);
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
        requireOwner(owner);
        requireId(id);
        return drafts.lockByIdAndOwner(id,owner)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireOwner(String owner) {
        if(owner==null||owner.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    private void requireId(Long id) {
        if(id==null||id<=0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private void validate(String owner,SocialPlatform platform,String title,
                          String body,LocalDateTime when) {
        requireOwner(owner);
        if(platform==null||title==null||title.isBlank()||
            title.length()>120||body==null||body.isBlank()||body.length()>3000)
            throw new IllegalArgumentException("Nội dung hoặc nền tảng không hợp lệ.");
        if(when!=null) {
            LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
            if(when.isBefore(now.plusMinutes(1))||when.isAfter(now.plusYears(1)))
                throw new IllegalArgumentException("Lịch đăng UTC phải từ 1 phút đến 1 năm trong tương lai.");
        }
    }
}
