package com.example.fbservice.security;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.notification.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountSessionService {
    private final AccountSessionRepository sessions;
    private final AccountSecurityEventRepository events;
    private final UserRepository users;
    private final AccountNotificationRepository notices;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public AccountSessionService(AccountSessionRepository sessions, AccountSecurityEventRepository events,
            UserRepository users, AccountNotificationRepository notices, PasswordEncoder encoder, Clock clock) {
        this.sessions=sessions;this.events=events;this.users=users;this.notices=notices;this.encoder=encoder;this.clock=clock;
    }

    @Transactional
    public void register(AccountPrincipal principal,HttpServletRequest request,String previousSessionId) {
        User account=users.lockById(principal.getAccountId()).filter(User::isEnabled)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        if(account.getAuthVersion()!=principal.getAuthVersion())throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var session=request.getSession();var now=LocalDateTime.now(clock);
        String ip=ClientMetadata.ip(request),device=ClientMetadata.device(request);
        boolean newClient=sessions.existsByUserId(account.getId())
            && !sessions.existsByUserIdAndIpAddressAndDeviceLabel(account.getId(),ip,device);
        if(previousSessionId!=null&&!previousSessionId.equals(session.getId())){
            sessions.lockByHash(ClientMetadata.hash(previousSessionId)).ifPresent(previous->{
                if(previous.valid(account.getId(),account.getAuthVersion(),now))previous.revoke(now);
            });
        }
        var active=active(account,now);
        // Serialize logins on the account row, keeping no more than ten active sessions.
        for(int i=0;i<=active.size()-10;i++)active.get(i).revoke(now);
        sessions.save(new AccountSession(account.getId(),ClientMetadata.hash(session.getId()),account.getAuthVersion(),
            ip,device,now,timeout(session)));
        events.save(new AccountSecurityEvent(account.getId(),SecurityEventType.LOGIN_SUCCESS,ip,device,now));
        if(newClient)notices.save(new AccountNotification(account.getUsername(),"Đăng nhập từ trình duyệt hoặc mạng mới",
            "Có đăng nhập mới lúc "+now+" UTC, IP "+ip+". Nếu không phải bạn, hãy đổi mật khẩu và thu hồi phiên tại Hồ sơ → Bảo mật."));
    }

    @Transactional
    public boolean validateAndTouch(AccountPrincipal principal,HttpSession session) {
        if(session==null)return false;
        var now=LocalDateTime.now(clock);
        return sessions.lockByHash(ClientMetadata.hash(session.getId())).map(record->{
            if(!record.valid(principal.getAccountId(),principal.getAuthVersion(),now))return false;
            record.touch(now,timeout(session));return true;
        }).orElse(false);
    }

    public record SessionView(Long id,String ipAddress,String deviceLabel,LocalDateTime createdAt,
                              LocalDateTime lastSeenAt,LocalDateTime expiresAt,boolean current){}

    @Transactional(readOnly=true)
    public List<SessionView> list(User account,String currentSessionId) {
        String hash=currentSessionId==null?"":ClientMetadata.hash(currentSessionId);
        return active(account,LocalDateTime.now(clock)).stream().map(s->new SessionView(s.getId(),s.getIpAddress(),
            s.getDeviceLabel(),s.getCreatedAt(),s.getLastSeenAt(),s.getExpiresAt(),s.matches(hash))).toList();
    }

    @Transactional(readOnly=true)
    public Page<AccountSecurityEvent> history(Long userId,int page) {
        if(page<0||page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trang không hợp lệ.");
        return events.findByUserId(userId,PageRequest.of(page,20,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
    }

    @Transactional
    public boolean revoke(String username,Long id,String password,HttpServletRequest request) {
        User account=users.lockByUsername(username).filter(User::isEnabled)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Look up ownership first; a foreign ID must not reveal whether a session exists.
        var record=sessions.lockOwned(id,account.getId())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(password==null||password.getBytes(StandardCharsets.UTF_8).length>72||!encoder.matches(password,account.getPassword()))
            throw new IllegalArgumentException("Mật khẩu hiện tại chưa đúng.");
        var now=LocalDateTime.now(clock);
        record.revoke(now);
        events.save(new AccountSecurityEvent(account.getId(),SecurityEventType.SESSION_REVOKED,
            ClientMetadata.ip(request),ClientMetadata.device(request),now));
        var session=request.getSession(false);
        return session!=null&&record.matches(ClientMetadata.hash(session.getId()));
    }

    @Transactional
    public void logout(AccountPrincipal principal,HttpServletRequest request) {
        var session=request.getSession(false);if(session==null)return;
        // Keep the same account → session lock order used by registration/revocation.
        if(users.lockById(principal.getAccountId()).isEmpty())return;
        sessions.lockByHash(ClientMetadata.hash(session.getId())).ifPresent(record->{
            var now=LocalDateTime.now(clock);
            if(record.valid(principal.getAccountId(),principal.getAuthVersion(),now)){
                record.revoke(now);
                events.save(new AccountSecurityEvent(principal.getAccountId(),SecurityEventType.LOGOUT,
                    ClientMetadata.ip(request),ClientMetadata.device(request),now));
            }
        });
    }

    private List<AccountSession> active(User account,LocalDateTime now){
        return sessions.findByUserIdAndAuthVersionAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAscIdAsc(
            account.getId(),account.getAuthVersion(),now);
    }
    private int timeout(HttpSession session){return session.getMaxInactiveInterval()>0?session.getMaxInactiveInterval():1800;}
}
