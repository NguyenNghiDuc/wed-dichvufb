package com.example.fbservice;

import com.example.fbservice.config.TrustedProxyConfiguration;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.AccountSecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:trustedproxy;DB_CLOSE_DELAY=-1",
 "server.forward-headers-strategy=native","server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1|0:0:0:0:0:0:0:1|::1"})
class TrustedProxyIntegrationTest {
 @Autowired TestRestTemplate client; @Autowired UserRepository users; @Autowired PasswordEncoder encoder;
 @Autowired AccountSecurityEventRepository events; @LocalServerPort int port;
 @Test void onlyExplicitlyTrustedProxyCanSupplyClientAddressToAuthentication(){
  User user=new User();user.setUsername("trusted_proxy_user");user.setPassword(encoder.encode("ProxyPass123!"));users.saveAndFlush(user);
  HttpHeaders headers=new HttpHeaders();headers.set("X-Forwarded-For","198.51.100.37");headers.set("X-Forwarded-Proto","https");
  String url="http://127.0.0.1:"+port;
  var form=client.exchange(url+"/login",HttpMethod.GET,new HttpEntity<>(headers),String.class);
  assertThat(form.getStatusCode().value()).isEqualTo(200);
  var token=java.util.regex.Pattern.compile("name=\\\"_csrf\\\"[^>]*value=\\\"([^\\\"]+)\\\"").matcher(form.getBody());
  assertThat(token.find()).isTrue();
  headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
  headers.set(HttpHeaders.COOKIE,form.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";",2)[0]);
  var body=new LinkedMultiValueMap<String,String>();body.add("username",user.getUsername());body.add("password","wrong-password");body.add("_csrf",token.group(1));
  client.exchange(url+"/login",HttpMethod.POST,new HttpEntity<>(body,headers),String.class);
  var history=events.findAll().stream().filter(e->e.getIpAddress().equals("198.51.100.37")).toList();
  assertThat(history).hasSize(1);
 }
 @Test void unsafeForwardingConfigurationIsRejected(){
  assertThatThrownBy(()->new TrustedProxyConfiguration("native","")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->new TrustedProxyConfiguration("framework","127\\.0\\.0\\.1")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->new TrustedProxyConfiguration("native",".*")).isInstanceOf(IllegalArgumentException.class);
 }
}
