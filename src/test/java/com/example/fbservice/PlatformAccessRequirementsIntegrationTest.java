package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:platformrequirements29;DB_CLOSE_DELAY=-1")
class PlatformAccessRequirementsIntegrationTest {
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="platform-reviewer-29")
    void fourPlatformsDescribeOfficialReviewNeedsWithoutInventingConnections() throws Exception {
        mvc.perform(get("/api/v1/integrations/requirements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].platform").value("FACEBOOK"))
            .andExpect(jsonPath("$[1].platform").value("INSTAGRAM"))
            .andExpect(jsonPath("$[2].platform").value("TIKTOK"))
            .andExpect(jsonPath("$[3].platform").value("YOUTUBE"))
            .andExpect(jsonPath("$[0].accountConnected").value(false))
            .andExpect(jsonPath("$[1].accountConnected").value(false))
            .andExpect(jsonPath("$[2].publishingEnabled").value(false))
            .andExpect(jsonPath("$[3].publishingEnabled").value(false))
            .andExpect(jsonPath("$[2].examplePublishScopes[0]").value("video.publish"))
            .andExpect(jsonPath("$[1].exampleReadScopes[0]").value("instagram_business_basic"))
            .andExpect(content().string(not(containsString("access_token"))))
            .andExpect(content().string(not(containsString("client_secret"))));
    }

    @Test
    void anonymousUsersCannotInspectConnectedAccountReadiness() throws Exception {
        mvc.perform(get("/api/v1/integrations/requirements"))
            .andExpect(status().is3xxRedirection());
    }
}
