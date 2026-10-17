package com.example.fbservice.content;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentDraftRepository extends JpaRepository<ContentDraft,Long> {
    List<ContentDraft> findByOwnerUsernameOrderByCreatedAtDesc(String username);
}
