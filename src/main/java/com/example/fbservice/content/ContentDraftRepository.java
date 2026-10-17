package com.example.fbservice.content;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentDraftRepository extends JpaRepository<ContentDraft,Long>,JpaSpecificationExecutor<ContentDraft> {
    // Legacy helper retained for compatibility. New web list always uses pagination.
    List<ContentDraft> findByOwnerUsernameOrderByCreatedAtDesc(String username);
    Page<ContentDraft> findByOwnerUsername(String username, Pageable pageable);
    Optional<ContentDraft> findByIdAndOwnerUsername(Long id, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ContentDraft d where d.id = :id and d.ownerUsername = :owner")
    Optional<ContentDraft> lockByIdAndOwner(@Param("id") Long id, @Param("owner") String owner);
}
