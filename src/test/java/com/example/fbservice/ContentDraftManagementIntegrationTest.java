package com.example.fbservice;

import com.example.fbservice.campaign.SocialPlatform;
import com.example.fbservice.content.ContentDraft;
import com.example.fbservice.content.ContentDraftService;
import com.example.fbservice.content.DraftStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:contentDraftManagement;DB_CLOSE_DELAY=-1")
class ContentDraftManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ContentDraftService drafts;

    private ContentDraft add(String owner,String title) {
        return drafts.save(owner,SocialPlatform.FACEBOOK,title,"Bài viết mẫu có nội dung hợp lệ",null);
    }

    @Test @WithMockUser(username="editor-owned")
    void editCanRescheduleAndClearThePlannedTime() throws Exception {
        ContentDraft draft=add("editor-owned","Bài viết ban đầu");
        LocalDateTime when=LocalDateTime.now(ZoneOffset.UTC).plusDays(5).withSecond(0).withNano(0);
        String scheduled=when.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        mvc.perform(get("/content-calendar/"+draft.getId()+"/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("content_draft_edit"))
            .andExpect(model().attributeExists("draft"));
        mvc.perform(post("/content-calendar/"+draft.getId()+"/edit").with(csrf())
                .param("platform","TIKTOK")
                .param("title","Tiêu đề đã sửa")
                .param("body","Nội dung đã cập nhật an toàn")
                .param("scheduledAtUtc",scheduled))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/content-calendar"));

        ContentDraft planned=drafts.findMineById("editor-owned",draft.getId());
        assertThat(planned.getPlatform()).isEqualTo(SocialPlatform.TIKTOK);
        assertThat(planned.getStatus()).isEqualTo(DraftStatus.PLANNED);
        assertThat(planned.getScheduledAtUtc()).isEqualTo(when);

        mvc.perform(post("/content-calendar/"+draft.getId()+"/edit").with(csrf())
                .param("platform","YOUTUBE").param("title","Bản nháp sau khi bỏ lịch")
                .param("body","Nội dung vẫn còn lưu nguyên vẹn"))
            .andExpect(status().is3xxRedirection());
        ContentDraft unscheduled=drafts.findMineById("editor-owned",draft.getId());
        assertThat(unscheduled.getStatus()).isEqualTo(DraftStatus.DRAFT);
        assertThat(unscheduled.getScheduledAtUtc()).isNull();
    }

    @Test @WithMockUser(username="editor-owner-safe")
    void editAndDeleteDenyOtherAccountsAndRequireCsrf() throws Exception {
        ContentDraft draft=add("editor-private","Bài riêng tư");
        mvc.perform(get("/content-calendar/"+draft.getId()+"/edit"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/content-calendar/"+draft.getId()+"/edit").with(csrf())
                .param("platform","FACEBOOK").param("title","Tấn công")
                .param("body","Không được phép sửa nội dung người khác"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/content-calendar/"+draft.getId()+"/delete").with(csrf()))
            .andExpect(status().isNotFound());
        assertThat(drafts.findMineById("editor-private",draft.getId()).getTitle())
            .isEqualTo("Bài riêng tư");

        ContentDraft mine=add("editor-owner-safe","Bài cần giữ");
        mvc.perform(post("/content-calendar/"+mine.getId()+"/delete"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/content-calendar/"+mine.getId()+"/edit")
                .param("platform","TIKTOK").param("title","Sai CSRF")
                .param("body","Không có token nên không được sửa"))
            .andExpect(status().isForbidden());
        assertThat(drafts.findMineById("editor-owner-safe",mine.getId()).getTitle())
            .isEqualTo("Bài cần giữ");
        mvc.perform(post("/content-calendar/"+mine.getId()+"/delete").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThatThrownBy(()->drafts.findMineById("editor-owner-safe",mine.getId()))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test @WithMockUser(username="editor-invalid")
    void invalidEditsDoNotMutateDataAndPaginationIsBounded() throws Exception {
        ContentDraft draft=add("editor-invalid","Tiêu đề hợp lệ");
        mvc.perform(post("/content-calendar/"+draft.getId()+"/edit").with(csrf())
                .param("platform","FACEBOOK").param("title","   ")
                .param("body","Nội dung hợp lệ"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("error"));
        mvc.perform(post("/content-calendar/"+draft.getId()+"/edit").with(csrf())
                .param("platform","FACEBOOK").param("title","Lịch sai")
                .param("body","Nội dung hợp lệ")
                .param("scheduledAtUtc",LocalDateTime.now(ZoneOffset.UTC).minusDays(1).toString()))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("error"));
        assertThat(drafts.findMineById("editor-invalid",draft.getId()).getTitle())
            .isEqualTo("Tiêu đề hợp lệ");

        mvc.perform(get("/content-calendar").param("page","0"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("pageNumber",0))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sửa")));
        mvc.perform(get("/content-calendar").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/content-calendar").param("page","10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/content-calendar").param("page","abc"))
            .andExpect(status().isBadRequest());
        assertThat(drafts.pageMine("editor-outsider",0).getTotalElements()).isZero();
    }

    @Test @WithMockUser(username="duplicate-owner")
    void duplicateRequiresCsrfAndCopiesOnlyOwnedDraftWithoutSchedule() throws Exception {
        ContentDraft original = drafts.save("duplicate-owner", SocialPlatform.FACEBOOK,
            "Bài gốc", "Nội dung bản gốc", LocalDateTime.now(ZoneOffset.UTC).plusDays(3));
        ContentDraft foreign = add("duplicate-other", "Bài riêng");

        mvc.perform(post("/content-calendar/" + original.getId() + "/duplicate"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/content-calendar/" + foreign.getId() + "/duplicate").with(csrf()))
            .andExpect(status().isNotFound());
        mvc.perform(post("/content-calendar/" + original.getId() + "/duplicate").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/content-calendar"));

        var copies = drafts.mine("duplicate-owner");
        assertThat(copies).hasSize(2);
        ContentDraft copy = copies.stream().filter(d -> !d.getId().equals(original.getId()))
            .findFirst().orElseThrow();
        assertThat(copy.getTitle()).isEqualTo(original.getTitle());
        assertThat(copy.getBody()).isEqualTo(original.getBody());
        assertThat(copy.getPlatform()).isEqualTo(original.getPlatform());
        assertThat(copy.getStatus()).isEqualTo(DraftStatus.DRAFT);
        assertThat(copy.getScheduledAtUtc()).isNull();
        assertThat(drafts.mine("duplicate-other")).hasSize(1);
    }

    @Test
    void anonymousCannotManageDrafts() throws Exception {
        mvc.perform(get("/content-calendar")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/content-calendar/1/edit")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/content-calendar/1/delete").with(csrf()))
            .andExpect(status().is3xxRedirection());
    }
}
