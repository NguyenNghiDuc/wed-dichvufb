package com.example.fbservice.admin;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
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
        return page(page, null, null, null);
    }

    /** Exact matching only: no wildcards, free-form JPQL or unbounded results. */
    @Transactional(readOnly=true)
    public Page<User> page(int page, String username, String role, Boolean enabled) {
        validatePage(page);
        String exactUsername = username == null || username.isBlank() ? null : username.trim();
        String exactRole = role == null || role.isBlank() ? null : role.trim();
        if (exactUsername != null && !exactUsername.matches("[a-zA-Z0-9._-]{3,40}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
        if (exactRole != null && !List.of("USER", "STAFF", "ADMIN").contains(exactRole))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");

        Specification<User> conditions = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (exactUsername != null) predicates.add(builder.equal(root.get("username"), exactUsername));
            if (exactRole != null) predicates.add(builder.equal(root.get("role"), exactRole));
            if (enabled != null) predicates.add(builder.equal(root.get("enabled"), enabled));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        return users.findAll(conditions,
            PageRequest.of(page, 20, Sort.by(Sort.Order.desc("id"))));
    }

    @Transactional(readOnly=true)
    public Page<AdminAccountAudit> pageAudit(int page) {
        return pageAudit(page, null, null, null);
    }

    /** Filter audit records using exact database-side predicates, never user SQL fragments. */
    @Transactional(readOnly=true)
    public Page<AdminAccountAudit> pageAudit(int page, String actor,
                                            Long targetId, String operation) {
        validatePage(page);
        String exactActor = actor == null || actor.isBlank() ? null : actor.trim();
        String exactOperation = operation == null || operation.isBlank() ? null : operation.trim();
        if (exactActor != null && !exactActor.matches("[a-zA-Z0-9._-]{3,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audit actor");
        if (targetId != null && targetId <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account ID");
        if (exactOperation != null &&
            !List.of("ACCOUNT_ENABLED", "ACCOUNT_ROLE").contains(exactOperation))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audit operation");

        Specification<AdminAccountAudit> spec = (root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            if (exactActor != null) filters.add(builder.equal(root.get("actor"), exactActor));
            if (targetId != null) filters.add(builder.equal(root.get("targetUserId"), targetId));
            if (exactOperation != null)
                filters.add(builder.equal(root.get("operation"), exactOperation));
            return builder.and(filters.toArray(new Predicate[0]));
        };
        return audit.findAll(spec, PageRequest.of(page, 20,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Transactional(readOnly=true)
    public List<AdminAccountAudit> recentAudit() {
        return pageAudit(0).getContent();
    }

    private static void validatePage(int page) {
        if (page < 0 || page > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
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
