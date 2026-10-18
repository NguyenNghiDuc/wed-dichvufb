package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.model.User;
import com.example.fbservice.support.SupportTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:chatSupportLimit27;DB_CLOSE_DELAY=-1")
class ChatSupportRateLimitIntegrationTest {
    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired SupportTicketService support;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="limit-chat-sender")
    void excessChatSendReturns429WithoutSaving() throws Exception {
        User receiver = new User();
        receiver.setUsername("limit-chat-recipient");
        receiver.setRole("USER");
        receiver.setEnabled(true);
        receiver.setPassword("$2a$10$testOnlyRateLimitPasswordHash");
        users.save(receiver);
        for (int i=0;i<120;i++)
            chat.saveMessage(new ChatMessage("limit-chat-sender","limit-chat-recipient","Message "+i));
        mvc.perform(post("/chat/send").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"receiver\":\"limit-chat-recipient\",\"content\":\"More\"}"))
            .andExpect(status().isTooManyRequests());
        assertThat(chat.threadFor("limit-chat-sender","limit-chat-recipient",0,20)
            .getTotalElements()).isEqualTo(120);
        chat.saveMessage(new ChatMessage("limit-other-sender","limit-chat-recipient","Independent"));
        assertThat(chat.threadFor("limit-other-sender","limit-chat-recipient",0,20)
            .getTotalElements()).isEqualTo(1);
    }

    @Test @WithMockUser(username="limit-support-sender")
    void excessTicketRepliesReturn429WithoutSaving() throws Exception {
        var ticket=support.create("limit-support-sender","Cần hỗ trợ phản hồi",
            "Nội dung để kiểm tra giới hạn gửi tin nhắn.");
        for (int i=0;i<60;i++)
            support.replyAsOwner("limit-support-sender",ticket.getId(),"Phản hồi "+i);
        mvc.perform(post("/support/"+ticket.getId()+"/reply").with(csrf())
            .param("message","More replies"))
            .andExpect(status().isTooManyRequests());
        assertThat(support.pageMessagesForOwner("limit-support-sender",ticket.getId(),0)
            .getTotalElements()).isEqualTo(60);
        var another=support.create("limit-support-other","Yêu cầu độc lập",
            "Người dùng khác vẫn có thể gửi phản hồi bình thường.");
        support.replyAsOwner("limit-support-other",another.getId(),"Tin nhắn khác");
        assertThat(support.pageMessagesForOwner("limit-support-other",another.getId(),0)
            .getTotalElements()).isEqualTo(1);
    }
}
