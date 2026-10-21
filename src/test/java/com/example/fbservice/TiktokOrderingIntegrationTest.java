package com.example.fbservice;

import com.example.fbservice.tiktok.*;
import com.example.fbservice.campaign.CampaignStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional @WithMockUser(username="tiktok_alice")
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:tiktokordering;DB_CLOSE_DELAY=-1")
class TiktokOrderingIntegrationTest {
    @Autowired TiktokOrderingService service;
    @Autowired TiktokOrderRepository orders;
    @Autowired MockMvc mvc;
    TiktokServer server;
    @BeforeEach void configure(){server=service.saveServer(null,"VN Manual","VN","Xét duyệt thủ công",12,100,10000,true,0);}
    TiktokOrderingService.Request request(){return new TiktokOrderingService.Request("https://www.tiktok.com/@sample","VN",server.getId(),100,"");}

    @Test void publicCatalogIsConsultingAndLegacyPreviewRedirects() throws Exception {
        mvc.perform(get("/").with(anonymous())).andExpect(status().isOk())
            .andExpect(content().string(containsString("gói tư vấn")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"tiktok-order-form\""))));
        mvc.perform(get("/tiktok/orders/new")).andExpect(redirectedUrl("/campaigns/new"));
        assertThat(orders.count()).isZero();
    }
    @Test void hostileLinkWrongCountryAndQuantityAreRejected(){
        for(String url:new String[]{"javascript:alert(1)","https://tiktok.com.evil.example/@sample","https://user@tiktok.com/@sample","https://tiktok.com:443/@sample","https://tiktok.com/no-profile"}){
            assertThatThrownBy(()->service.quote(new TiktokOrderingService.Request(url,"VN",server.getId(),100,""))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(()->service.quote(new TiktokOrderingService.Request(request().targetUrl(),"US",server.getId(),100,""))).isInstanceOf(IllegalArgumentException.class);
        for(int quantity:new int[]{0,99,10001,Integer.MAX_VALUE}){
            assertThatThrownBy(()->service.quote(new TiktokOrderingService.Request(request().targetUrl(),"VN",server.getId(),quantity,""))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(orders.count()).isZero();
    }
    @Test void serverChangesCannotRewriteHistoricOrderOrAcceptOldQuote(){
        var order=service.create("tiktok_alice",request(),0);
        service.saveServer(server.getId(),"Updated VN","VN","Changed",20,100,10000,true,0);
        // Force the version bump before quoting the updated configuration.
        var updated=service.allServers().get(0);
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(order.getUnitPriceVnd()).isEqualTo(12);
        assertThat(order.getTotalPriceVnd()).isEqualTo(1200);
        assertThat(order.getServerName()).isEqualTo("VN Manual");
        assertThat(service.quote(request()).totalPriceVnd()).isEqualTo(2000);
        assertThatThrownBy(()->service.create("tiktok_alice",request(),0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(orders.count()).isEqualTo(1);
    }
    @Test void customerCannotTamperWithPriceOrReadAnotherOwnersOrder() throws Exception {
        mvc.perform(post("/tiktok/orders").with(csrf()).param("targetUrl",request().targetUrl()).param("country","VN")
            .param("serverId",server.getId().toString()).param("quantity","100").param("serverVersion","0"))
            .andExpect(status().isGone());
        assertThat(orders.count()).isZero();
        // A historical fixture still obeys owner scoping, snapshot prices and cancellation.
        var order=service.create("tiktok_alice",request(),0);
        assertThat(order.getTotalPriceVnd()).isEqualTo(1200);
        mvc.perform(get("/tiktok/orders/"+order.getId())).andExpect(status().isOk());
        mvc.perform(get("/tiktok/orders/"+order.getId()).with(user("tiktok_bob"))).andExpect(status().isNotFound());
        mvc.perform(post("/tiktok/orders/"+order.getId()+"/cancel")).andExpect(status().isForbidden());
        mvc.perform(post("/tiktok/orders/"+order.getId()+"/cancel").with(user("tiktok_bob")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/admin/tiktok/servers")).andExpect(status().isForbidden());
    }
    @Test void cancellationAndAdminStatusTransitionsAreRecorded() throws Exception {
        var order=service.create("tiktok_alice",request(),0);
        mvc.perform(get("/admin/tiktok/orders/"+order.getId()).with(user("operator").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/admin/tiktok/orders/"+order.getId()+"/status").with(user("operator").roles("ADMIN")).with(csrf()).param("status","APPROVED"))
            .andExpect(status().is3xxRedirection());
        assertThatThrownBy(()->service.cancel("tiktok_alice",order.getId())).isInstanceOf(IllegalArgumentException.class);
        service.updateStatus("operator",order.getId(),CampaignStatus.IN_PROGRESS);
        service.updateStatus("operator",order.getId(),CampaignStatus.COMPLETED);
        assertThat(service.history(order.getId(),0).getTotalElements()).isEqualTo(4);
        assertThat(order.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
        var cancellable=service.create("tiktok_alice",request(),0);
        mvc.perform(post("/tiktok/orders/"+cancellable.getId()+"/cancel").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(cancellable.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
    }
    @Test void serverAdminValidatesInputsAndClosedServerRejectsNewOrders() throws Exception {
        mvc.perform(get("/admin/tiktok/servers").with(user("operator").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/admin/tiktok/servers").with(user("operator").roles("ADMIN")).with(csrf())
            .param("name","Bad").param("country","XX").param("price","0").param("minimum","10").param("maximum","1"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
            .andExpect(content().string(containsString("value=\"Bad\"")));
        service.saveServer(server.getId(),"VN Manual","VN","Closed",12,100,10000,false,0);
        assertThat(service.available()).isEmpty();
        assertThatThrownBy(()->service.create("tiktok_alice",request(),0)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void orderListsArePaginatedAndOwnerScoped() throws Exception {
        for(int i=0;i<21;i++)service.create("tiktok_alice",request(),0);
        service.create("tiktok_bob",request(),0);
        assertThat(service.page("tiktok_alice",null,null,0).getContent()).hasSize(20);
        assertThat(service.page("tiktok_alice",null,null,1).getContent()).hasSize(1);
        assertThat(service.page("tiktok_alice",null,null,0).getTotalElements()).isEqualTo(21);
        mvc.perform(get("/tiktok/orders")).andExpect(status().isOk());
        mvc.perform(get("/admin/tiktok/orders").with(user("operator").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/tiktok/orders").param("page","-1")).andExpect(status().isBadRequest());
    }
    @Test void bothLegacyCreationRoutesAreClosedButCsrfStillProtectsThem() throws Exception {
        mvc.perform(get("/order/new")).andExpect(redirectedUrl("/campaigns/new"));
        mvc.perform(post("/order/save")).andExpect(status().isForbidden());
        mvc.perform(post("/order/save").with(csrf())).andExpect(status().isGone());
        mvc.perform(post("/tiktok/orders")).andExpect(status().isForbidden());
        mvc.perform(post("/tiktok/orders").with(csrf())).andExpect(status().isGone());
        assertThat(orders.count()).isZero();
    }

}
