package com.example.fbservice.admin;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final AdminAccountAuditRepository audit;

    public AdminUserService(UserRepository users, AdminAccountAuditRepository audit) {
        this.users=users;
        this.audit=audit;
    }

    @Transactional(readOnly=true)
    public Page<User> page(int page) {
        if(page<0||page>10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page");
        return users.findAllByOrderByIdDesc(PageRequest.of(page,20));
    }

    @Transactional(readOnly=true)
    public List<AdminAccountAudit> recentAudit() {
        return audit.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional
    public void setEnabled(String actor, Long targetId, boolean enabled) {
        User target=lockAndAuthorize(actor,targetId);
        if(target.isEnabled()==enabled)
            throw new IllegalArgumentException("Tài khoản đang ở trạng thái này.");
        String before=Boolean.toString(target.isEnabled());
        target.setEnabled(enabled);
        audit.save(new AdminAccountAudit(actor,target.getId(),"ACCOUNT_ENABLED",before,Boolean.toString(enabled)));
    }

    @Transactional
    public void setRole(String actor, Long targetId, String role) {
        if(!List.of("USER","STAFF","ADMIN").contains(role))
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        User target=lockAndAuthorize(actor,targetId);
        if(role.equals(target.getRole()))
            throw new IllegalArgumentException("Tài khoản đã có vai trò này.");
        String before=target.getRole();
        target.setRole(role);
        audit.save(new AdminAccountAudit(actor,target.getId(),"ACCOUNT_ROLE",before,role));
    }

    private User lockAndAuthorize(String actor,Long targetId) {
        User operator=users.findByUsername(actor)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        if(!operator.isEnabled()||!"ADMIN".equals(operator.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(targetId==null||operator.getId().equals(targetId))
            throw new IllegalArgumentException("Không được tự thay đổi quyền hoặc trạng thái của mình.");
        // Lock enabled admins before editing, preserving an active administrator.
        users.lockEnabledAdmins();
        return users.lockById(targetId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
