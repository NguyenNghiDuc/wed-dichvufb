package com.example.fbservice;

import com.example.fbservice.campaign.SocialPlatform;
import com.example.fbservice.content.ContentDraftService;
import com.example.fbservice.content.DraftStatus;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:contentsearch;DB_CLOSE_DELAY=-1")
class ContentSearchIntegrationTest {
    @Autowired ContentDraftService drafts;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="search-alpha")
    void platformStatusAndSearchAlwaysRespectOwnership() throws Exception {
        drafts.save("search-alpha",SocialPlatform.TIKTOK,
            "Launch video", "Tips for students",null);
        drafts.save("search-alpha",SocialPlatform.TIKTOK,
            "Launch planned", "Do not show draft filter",
            LocalDateTime.now(ZoneOffset.UTC).plusDays(3));
        drafts.save("search-alpha",SocialPlatform.FACEBOOK,
            "Launch facebook", "Different network",null);
        drafts.save("search-beta",SocialPlatform.TIKTOK,
            "SECRET_FOREIGN_Launch", "Not owned by search-alpha",null);

        var filtered=drafts.searchMine("search-alpha",0,SocialPlatform.TIKTOK,DraftStatus.DRAFT,"Launch");
        assertThat(filtered.getTotalElements()).isEqualTo(1);
        assertThat(filtered.getContent().get(0).getTitle()).isEqualTo("Launch video");

        mvc.perform(get("/content-calendar").param("platform","TIKTOK")
                .param("status","DRAFT").param("q","Launch"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Launch video")))
            .andExpect(content().string(not(containsString("SECRET_FOREIGN_Launch"))))
            .andExpect(content().string(not(containsString("Launch planned"))));
        assertThat(drafts.searchMine("search-alpha",0,null,null,"SECRET_FOREIGN").getTotalElements())
            .isZero();
    }

    @Test @WithMockUser(username="search-literal")
    void wildcardCharactersAreTreatedAsLiteralText() throws Exception {
        drafts.save("search-literal",SocialPlatform.YOUTUBE,
            "Save 100%_today", "Real marketing analysis",null);
        drafts.save("search-literal",SocialPlatform.YOUTUBE,
            "Save 100xtoday", "Different text",null);

        assertThat(drafts.searchMine("search-literal",0,null,null,"100%_today")
            .getTotalElements()).isEqualTo(1);
        assertThat(drafts.searchMine("search-literal",0,null,null,"100%")
            .getTotalElements()).isEqualTo(1);
        assertThat(drafts.searchMine("search-literal",0,null,null,"NOT FOUND")
            .getTotalElements()).isZero();
        assertThat(drafts.searchMine("search-literal",0,null,null,"marketing")
            .getTotalElements()).isEqualTo(1);
    }

    @Test @WithMockUser(username="search-validate")
    void rejectsUnboundedQueriesAndInvalidEnumsAndEscapesHTML() throws Exception {
        mvc.perform(get("/content-calendar").param("q","A".repeat(101)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/content-calendar").param("q","broken\nvalue"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/content-calendar").param("platform","INVALID"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/content-calendar").param("status","POSTED"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/content-calendar").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/content-calendar").param("q","<script>alert(1)</script>"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("value=\"<script>"))));
    }

    @Test @WithMockUser(username="search-pages")
    void paginationPreservesTheActiveSearchFilters() throws Exception {
        for(int i=0;i<21;i++)
            drafts.save("search-pages",SocialPlatform.YOUTUBE,
                "Course tip "+i, "Teaching advice",null);
        mvc.perform(get("/content-calendar").param("platform","YOUTUBE")
                .param("status","DRAFT").param("q","Course"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("hasNext",true))
            .andExpect(content().string(containsString("q=Course")));
        assertThat(drafts.searchMine("search-pages",1,SocialPlatform.YOUTUBE,DraftStatus.DRAFT,"Course")
            .getNumberOfElements()).isEqualTo(1);
    }

    @Test
    void anonymousSearchMustAuthenticate() throws Exception {
        mvc.perform(get("/content-calendar").param("q","anything"))
            .andExpect(status().is3xxRedirection());
    }
    @Test @WithMockUser(username="search-ai-form")
    void assistantPreviewIsAccessibleButNeverPostsAutomatically() throws Exception {
        mvc.perform(get("/content-calendar"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Trợ lý viết nội dung")))
            .andExpect(content().string(containsString("content-assistant.js")))
            .andExpect(content().string(containsString("copy-ai-to-draft")))
            .andExpect(content().string(containsString("create-content-form")))
            .andExpect(content().string(containsString("data-plan-fallback-url")))
            .andExpect(content().string(containsString("content-metrics.js")))
            .andExpect(content().string(containsString("không xác minh số liệu")))
            .andExpect(content().string(containsString("Xóa bộ lọc")));
        mvc.perform(get("/content-assistant.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("credentials: \"same-origin\"")))
            .andExpect(content().string(containsString("result.value = responseData.content")))
            .andExpect(content().string(containsString("draftBody.value = draftText")))
            .andExpect(content().string(containsString("template.suggestions.map")))
            .andExpect(content().string(not(containsString("draftForm.submit"))))
            .andExpect(content().string(not(containsString("innerHTML"))));
    }

}
