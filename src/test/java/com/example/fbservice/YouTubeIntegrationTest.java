package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:youtubetest;DB_CLOSE_DELAY=-1",
    "growth.youtube.api-key="})
class YouTubeIntegrationTest {
    @Autowired MockMvc mvc;
    @Test @WithMockUser
    void rejectUntrustedIdAndFailClosedWithoutApiKey() throws Exception{
        mvc.perform(get("/api/v1/integrations/youtube/channels/bad-id"))
           .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/integrations/youtube/channels/UCxxxxxxxxxxxxxxxxxxxxxx"))
           .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/integrations"))
           .andExpect(status().isOk()).andExpect(jsonPath("$[0].publishingEnabled").value(false));
    }
    @Test
    void anonymousCannotUseIntegration() throws Exception{
        mvc.perform(get("/api/v1/integrations")).andExpect(status().is3xxRedirection());
    }
}
