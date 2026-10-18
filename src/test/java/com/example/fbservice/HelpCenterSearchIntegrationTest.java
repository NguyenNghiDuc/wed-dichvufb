package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Black-box regression checks for public FAQ search/filtering. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:helpcentersearch;DB_CLOSE_DELAY=-1")
class HelpCenterSearchIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test
    void helpIsPublicAndShowsSearchAndAllCategories() throws Exception {
        mvc.perform(get("/help"))
            .andExpect(status().isOk())
            .andExpect(view().name("help"))
            .andExpect(model().attribute("count", is(14)))
            .andExpect(content().string(containsString("Câu hỏi thường gặp")))
            .andExpect(content().string(containsString("Tìm kiếm")))
            .andExpect(content().string(containsString("Tất cả chủ đề")));
    }

    @Test
    void accentInsensitiveSearchMatchesVietnameseQuestion() throws Exception {
        mvc.perform(get("/help").param("q", "huy don"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("count", is(1)))
            .andExpect(content().string(containsString("Có thể hủy đơn hàng không?")));
    }

    @Test
    void categoryFilterLimitsResultsWithoutLogin() throws Exception {
        mvc.perform(get("/help").param("category", "payment"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("selectedCategory", "payment"))
            .andExpect(model().attribute("count", is(2)))
            .andExpect(content().string(containsString("Tôi có thể nạp tiền thật")))
            .andExpect(content().string(not(containsString("Có thể hủy đơn hàng"))));
    }

    @Test
    void categoryAndQueryCombineAsIntersection() throws Exception {
        mvc.perform(get("/help").param("category", "account").param("q", "OTP"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("count", is(1)))
            .andExpect(content().string(containsString("Tôi quên mật khẩu")));
    }

    @Test
    void emptySearchHasAccessibleFallback() throws Exception {
        mvc.perform(get("/help").param("q", "unmatched-no-answer-0099"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("count", is(0)))
            .andExpect(content().string(containsString("Không tìm thấy câu hỏi phù hợp")));
    }

    @Test
    void rejectsUnknownCategoryAndOverlongQuery() throws Exception {
        mvc.perform(get("/help").param("category", "admin"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/help").param("q", "a".repeat(81)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchValueIsEscapedInsteadOfBecomingMarkup() throws Exception {
        mvc.perform(get("/help").param("q", "<img src=x onerror=alert(1)>"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("<img src=x onerror=alert(1)>"))))
            .andExpect(content().string(containsString("&lt;img")));
    }
}
