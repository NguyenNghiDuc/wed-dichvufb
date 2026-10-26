package com.example.fbservice.security;

import com.example.fbservice.config.DatabaseUserDetailsService;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.time.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Shared source/account throttling; account-row locking preserves failure audit counters. */
@Component
public class GuardedAuthenticationProvider extends DaoAuthenticationProvider {
 private final UserRepository users;
 private final AccountSecurityEventRepository events;
 private final Clock clock;
 private final com.example.fbservice.config.TrafficLimiter limiter;
 public GuardedAuthenticationProvider(DatabaseUserDetailsService details,PasswordEncoder encoder,
     UserRepository users,AccountSecurityEventRepository events,Clock clock,com.example.fbservice.config.TrafficLimiter limiter){
  setUserDetailsService(details);setPasswordEncoder(encoder);this.users=users;this.events=events;this.clock=clock;this.limiter=limiter;
 }
 @Override
 @Transactional(noRollbackFor=AuthenticationException.class)
 public Authentication authenticate(Authentication authentication)throws AuthenticationException{
  User account=users.lockByUsername(authentication.getName()).orElse(null);
  LocalDateTime now=LocalDateTime.now(clock);
  LoginRequestDetails client=authentication.getDetails() instanceof LoginRequestDetails d?d:null;
  String source=client==null?"unknown":client.getRemoteAddress();
  String bucket="login-failure:"+authentication.getName()+":"+source;
  try{limiter.check(bucket,5,Duration.ofMinutes(15));}
  catch(org.springframework.web.server.ResponseStatusException blocked){
   if(account!=null)event(account,authentication,SecurityEventType.LOGIN_BLOCKED,now);
   throw new LockedException("Account temporarily unavailable from this source");
  }
  try{
   Authentication result=super.authenticate(authentication);
   limiter.clear(bucket);
   if(account!=null){account.setFailedLoginCount(0);account.setFailedLoginWindow(null);account.setLoginLockedUntil(null);}
   return result;
  }catch(BadCredentialsException failure){
   if(account!=null&&account.isEnabled()){
    if(account.getFailedLoginWindow()==null||!account.getFailedLoginWindow().plusMinutes(15).isAfter(now)){
     account.setFailedLoginWindow(now);account.setFailedLoginCount(0);
    }
    account.setFailedLoginCount(account.getFailedLoginCount()+1);
    if(account.getFailedLoginCount()>=5)account.setLoginLockedUntil(now.plusMinutes(15));
    event(account,authentication,SecurityEventType.LOGIN_FAILURE,now);
   }
   throw failure;
  }
 }
 private void event(User user,Authentication authentication,SecurityEventType type,LocalDateTime now){
  LoginRequestDetails details=authentication.getDetails() instanceof LoginRequestDetails d?d:null;
  events.save(new AccountSecurityEvent(user.getId(),type,details==null?"":details.getRemoteAddress(),
      details==null?"":details.device(),now));
 }
}
