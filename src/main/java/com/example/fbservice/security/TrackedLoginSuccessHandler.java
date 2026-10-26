package com.example.fbservice.security;
import com.example.fbservice.config.AccountPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
@Component
public class TrackedLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
 private final AccountSessionService sessions;
 public TrackedLoginSuccessHandler(AccountSessionService sessions){
  this.sessions=sessions;setDefaultTargetUrl("/");setAlwaysUseDefaultTargetUrl(true);
  // Keep the external HTTPS origin when Tomcat is behind a Codespaces proxy.
  // sendRedirect would expand the URL using the internal HTTP connection.
  setRedirectStrategy((request,response,url)->{
   response.setStatus(HttpServletResponse.SC_SEE_OTHER);
   response.setHeader("Location",request.getContextPath()+"/");
   response.setHeader("Cache-Control","no-store");
  });
 }
 @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)
     throws IOException,ServletException{
  try{sessions.register((AccountPrincipal)authentication.getPrincipal(),request,
      authentication.getDetails() instanceof LoginRequestDetails details?details.getSessionId():null);}
  catch(RuntimeException failure){
   new SecurityContextLogoutHandler().logout(request,response,authentication);
   response.sendError(503,"Không thể tạo phiên an toàn. Vui lòng thử lại.");return;
  }
  super.onAuthenticationSuccess(request,response,authentication);
 }
}
