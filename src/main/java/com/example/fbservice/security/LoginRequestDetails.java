package com.example.fbservice.security;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
public final class LoginRequestDetails extends WebAuthenticationDetails {
 private final String device;
 public LoginRequestDetails(HttpServletRequest request){super(request);device=ClientMetadata.device(request);}
 public String device(){return device;}
}
