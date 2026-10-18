package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentDraftValidationTest {
    private final ContentDraftRepository repository = mock(ContentDraftRepository.class);
    private final ContentDraftService service = new ContentDraftService(repository);
    private final SocialPlatform platform = SocialPlatform.values()[0];

    @Test
    void blankOwnerIsRejectedBeforePersistence() {
        assertThrows(ResponseStatusException.class,
            () -> service.save(" ", platform, "Title", "Body", null));
        verifyNoInteractions(repository);
    }

    @Test
    void excessiveTitleAndBodyAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> service.save("owner", platform, "x".repeat(121), "Body", null));
        assertThrows(IllegalArgumentException.class,
            () -> service.save("owner", platform, "Title", "x".repeat(3001), null));
        verifyNoInteractions(repository);
    }

    @Test
    void invalidScheduleIsRejected() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        assertThrows(IllegalArgumentException.class,
            () -> service.save("owner", platform, "Title", "Body", now.minusDays(1)));
        assertThrows(IllegalArgumentException.class,
            () -> service.save("owner", platform, "Title", "Body", now.plusYears(2)));
        verifyNoInteractions(repository);
    }

    @Test
    void invalidPageIsRejectedBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.pageMine("owner", -1));
        assertThrows(IllegalArgumentException.class, () -> service.pageMine("owner", 10001));
        verifyNoInteractions(repository);
    }

    @Test
    void blankOwnerCannotReadDrafts() {
        assertThrows(ResponseStatusException.class, () -> service.mine(" "));
        assertThrows(ResponseStatusException.class, () -> service.pageMine(" ", 0));
        verifyNoInteractions(repository);
    }

    @Test
    void invalidDraftIdsAreRejectedBeforeQuery() {
        assertThrows(ResponseStatusException.class, () -> service.findMineById("owner", 0L));
        assertThrows(ResponseStatusException.class, () -> service.deleteMine("owner", -1L));
        verifyNoInteractions(repository);
    }

    @Test
    void duplicateRequiresOwnershipAndValidId() {
        assertThrows(ResponseStatusException.class, () -> service.duplicateMine(" ", 1L));
        assertThrows(ResponseStatusException.class, () -> service.duplicateMine("owner", -1L));
        when(repository.findByIdAndOwnerUsername(8L, "owner")).thenReturn(java.util.Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.duplicateMine("owner", 8L));
        verify(repository, never()).save(any(ContentDraft.class));
    }

    @Test
    void duplicateCreatesUnscheduledCopyForSameOwner() {
        ContentDraft original = new ContentDraft("owner", platform, "Original", "Body", LocalDateTime.now(ZoneOffset.UTC).plusDays(2));
        when(repository.findByIdAndOwnerUsername(9L, "owner")).thenReturn(java.util.Optional.of(original));
        when(repository.save(any(ContentDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ContentDraft duplicate = service.duplicateMine("owner", 9L);
        assertNotSame(original, duplicate);
        assertEquals("Original", duplicate.getTitle());
        assertEquals("Body", duplicate.getBody());
        assertNull(duplicate.getScheduledAtUtc());
        verify(repository).save(any(ContentDraft.class));
    }

    @Test
    void missingOwnedDraftCannotBeDeleted() {
        when(repository.lockByIdAndOwner(42L, "owner")).thenReturn(java.util.Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.deleteMine("owner", 42L));
        verify(repository, never()).delete(any(ContentDraft.class));
    }
}
