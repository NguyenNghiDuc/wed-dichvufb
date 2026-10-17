package com.example.fbservice;

import com.example.fbservice.content.*;
import com.example.fbservice.campaign.SocialPlatform;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:contenttest;DB_CLOSE_DELAY=-1",
    "growth.ai.api-key="})
class ContentIntegrationTest {
    @Autowired ContentDraftService drafts;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="content-owner")
    void saveDraftAndViewOwnCalendar() throws Exception{
        var draft=drafts.save("content-owner",SocialPlatform.FACEBOOK,"Mẹo đọc sách",
            "Đọc sách đều đặn mỗi tối",null);
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.DRAFT);
        assertThat(drafts.mine("content-owner")).hasSizeGreaterThanOrEqualTo(1);
        assertThat(drafts.mine("stranger")).isEmpty();
        mvc.perform(get("/content-calendar")).andExpect(status().isOk());
        mvc.perform(post("/content-calendar").with(csrf()).param("platform","TIKTOK")
            .param("title","Bài viết demo").param("body","Nội dung hữu ích"))
            .andExpect(status().is3xxRedirection());
        assertThatThrownBy(()->drafts.save("content-owner",SocialPlatform.YOUTUBE,"Title",
            "Content",LocalDateTime.now(ZoneOffset.UTC).minusDays(2)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @WithMockUser
    void aiWithoutSecretFailsClosed() throws Exception{
        mvc.perform(post("/api/v1/ai/caption").with(csrf())
            .contentType("application/json")
            .content("{\"platform\":\"TIKTOK\",\"topic\":\"giay the thao\",\"audience\":\"khach hang tre\"}"))
            .andExpect(status().isServiceUnavailable());
    }
}
