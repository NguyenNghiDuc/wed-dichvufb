package com.example.fbservice;

import com.example.fbservice.controller.HelpFaqService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:publichelpsearch;DB_CLOSE_DELAY=-1")
class HelpFaqSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HelpFaqService faqs;

    @Test
    void anonymousCanSearchCuratedPublicFaqWithoutSignIn() throws Exception {
        mvc.perform(get("/help"))
            .andExpect(status().isOk())
            .andExpect(view().name("help"))
            .andExpect(model().attribute("resultCount", greaterThan(9)))
            .andExpect(content().string(containsString("Câu hỏi thường gặp")))
            .andExpect(content().string(containsString("name=\"q\"")))
            .andExpect(content().string(containsString("name=\"category\"")));
    }

    @Test
    void findsUnaccentedVietnameseTermsAndCategoryFilters() throws Exception {
        mvc.perform(get("/help").param("q","huy don").param("category","campaign"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("faqs", hasSize(1)))
            .andExpect(model().attribute("resultCount", 1))
            .andExpect(content().string(containsString("Hủy đơn được không?")))
            .andExpect(content().string(not(containsString("Lịch nội dung có tự đăng bài không?"))));
        mvc.perform(get("/help").param("category","payment"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("faqs", hasSize(1)))
            .andExpect(content().string(containsString("Chức năng nạp tiền")));
        assertThat(faqs.search("lich noi dung","content")).hasSize(2);
    }

    @Test
    void noMatchShowsUsefulEmptyStateAndPreservesSelection() throws Exception {
        mvc.perform(get("/help").param("q","khongcodapandung").param("category","support"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("resultCount", 0))
            .andExpect(content().string(containsString("Không tìm thấy câu hỏi phù hợp")));
    }

    @Test
    void inputBoundedAndAllCategoriesValidated() throws Exception {
        mvc.perform(get("/help").param("q", "x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/help").param("category","private"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/help").param("category","x".repeat(100)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username="help-viewer",roles="USER")
    void escapedSearchCannotInjectExecutableHtml() throws Exception {
        String payload="<script>alert(1)</script>";
        mvc.perform(get("/help").param("q",payload))
            .andExpect(status().isOk())
            .andExpect(model().attribute("resultCount",0))
            .andExpect(content().string(not(containsString(payload))))
            .andExpect(content().string(containsString("Tìm câu trả lời")));
    }
}
