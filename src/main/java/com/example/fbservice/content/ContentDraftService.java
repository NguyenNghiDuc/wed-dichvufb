package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentDraftService {
    private final ContentDraftRepository drafts;
    public ContentDraftService(ContentDraftRepository drafts){this.drafts=drafts;}
    @Transactional
    public ContentDraft save(String owner,SocialPlatform platform,String title,String body,LocalDateTime when){
        if(owner==null||owner.isBlank()||platform==null||title==null||title.isBlank()||
            title.length()>120||body==null||body.isBlank()||body.length()>3000)
            throw new IllegalArgumentException("Nội dung hoặc nền tảng không hợp lệ.");
        if(when!=null) {
            LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
            if(when.isBefore(now.plusMinutes(1))||when.isAfter(now.plusYears(1)))
                throw new IllegalArgumentException("Lịch đăng UTC phải từ 1 phút đến 1 năm trong tương lai.");
        }
        return drafts.save(new ContentDraft(owner,platform,title.strip(),body.strip(),when));
    }
    @Transactional(readOnly=true)
    public List<ContentDraft> mine(String owner){return drafts.findByOwnerUsernameOrderByCreatedAtDesc(owner);}
}
