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

/** Account-row lock makes the failure window shared and atomic across application instances. */
@Component
public class GuardedAuthenticationProvider extends DaoAuthenticationProvider {
 private final UserRepository users;
 private final AccountSecurityEventRepository events;
 private final Clock clock;
 public GuardedAuthenticationProvider(DatabaseUserDetailsService details,PasswordEncoder encoder,
     UserRepository users,AccountSecurityEventRepository events,Clock clock){
  setUserDetailsService(details);setPasswordEncoder(encoder);this.users=users;this.events=events;this.clock=clock;
 }
 @Override
 @Transactional(noRollbackFor=AuthenticationException.class)
 public Authentication authenticate(Authentication authentication)throws AuthenticationException{
  User account=users.lockByUsername(authentication.getName()).orElse(null);
  LocalDateTime now=LocalDateTime.now(clock);
  if(account!=null&&account.getLoginLockedUntil()!=null&&account.getLoginLockedUntil().isAfter(now)){
   event(account,authentication,SecurityEventType.LOGIN_BLOCKED,now);
   throw new LockedException("Account temporarily unavailable");
  }
  try{
   Authentication result=super.authenticate(authentication);
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
