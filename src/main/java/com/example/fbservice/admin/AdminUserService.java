package com.example.fbservice.admin;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import com.example.fbservice.web.SearchText;
import org.springframework.data.jpa.domain.Specification;
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
        return page(page,null,null,null);
    }

    @Transactional(readOnly=true)
    public Page<User> page(int page,String query,String role,Boolean enabled) {
        if(page<0||page>10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page");
        String q=SearchText.clean(query,120);
        String roleFilter=SearchText.clean(role,16);
        if(!roleFilter.isEmpty()&&!List.of("USER","STAFF","ADMIN").contains(roleFilter))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Vai trò lọc không hợp lệ.");
        Specification<User> filter=(root,request,builder)->{
            var conditions=new ArrayList<Predicate>();
            if(!q.isEmpty()){
                String pattern=SearchText.contains(q);
                conditions.add(builder.or(builder.like(builder.lower(root.get("username")),pattern,'\\'),
                    builder.like(builder.lower(root.get("displayName")),pattern,'\\'),
                    builder.like(builder.lower(root.get("email")),pattern,'\\')));
            }
            if(!roleFilter.isEmpty())conditions.add(builder.equal(root.get("role"),roleFilter));
            if(enabled!=null)conditions.add(builder.equal(root.get("enabled"),enabled));
            return builder.and(conditions.toArray(new Predicate[0]));
        };
        return users.findAll(filter,PageRequest.of(page,20,Sort.by(Sort.Direction.DESC,"id")));
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
