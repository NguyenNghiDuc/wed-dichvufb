package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignStatus;
import com.example.fbservice.model.User;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminStaffDirectoryService {
    public enum RoleFilter { ALL, STAFF, ADMIN }
    public record StaffRow(String username, String role, boolean enabled, long activeOrders) {}
    public record Result(List<StaffRow> rows, boolean hasNext, int page, RoleFilter role) {}
    private final EntityManager em;

    public AdminStaffDirectoryService(EntityManager em) {
        this.em = em;
    }

    /** Paged staff directory, with a single grouped workload lookup for the current page. */
    @Transactional(readOnly = true)
    public Result page(int page, RoleFilter role, String exactUsername) {
        return page(page, role, exactUsername, null);
    }

    @Transactional(readOnly = true)
    public Result page(int page, RoleFilter role, String exactUsername, Boolean enabled) {
        if (page < 0 || page > 10000 || role == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or role");
        String username = exactUsername == null || exactUsername.isBlank() ?
            null : exactUsername.trim();
        if (username != null && !username.matches("[a-zA-Z0-9._-]{3,40}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid staff username");

        String where = (username == null ? "" : " and u.username = :username") +
            (enabled == null ? "" : " and u.enabled = :enabled");
        var users = em.createQuery("select u from User u where u.role in :roles" +
            where + " order by u.username asc, u.id asc", User.class)
            .setParameter("roles", role == RoleFilter.ALL ?
                List.of("STAFF", "ADMIN") : List.of(role.name()));
        if (username != null) users.setParameter("username", username);
        if (enabled != null) users.setParameter("enabled", enabled);
        List<User> selected = users.setFirstResult(page * 20)
            .setMaxResults(21).getResultList();
        boolean hasNext = selected.size() > 20;
        var visible = selected.subList(0, Math.min(20, selected.size()));
        if (visible.isEmpty()) return new Result(List.of(), hasNext, page, role);

        List<String> usernames = visible.stream().map(User::getUsername).toList();
        List<Object[]> assignments = em.createQuery("""
             select c.assigneeUsername, count(c.id) from CampaignOrder c
             where c.assigneeUsername in :staff
               and c.status in :active
             group by c.assigneeUsername
             """, Object[].class)
            .setParameter("staff", usernames)
            .setParameter("active", List.of(CampaignStatus.APPROVED, CampaignStatus.IN_PROGRESS))
            .getResultList();
        Map<String, Long> loads = new HashMap<>();
        for (Object[] value : assignments)
            loads.put((String)value[0], ((Number)value[1]).longValue());
        return new Result(visible.stream()
            .map(u -> new StaffRow(u.getUsername(), u.getRole(), u.isEnabled(),
                                   loads.getOrDefault(u.getUsername(), 0L)))
            .toList(), hasNext, page, role);
    }
}
