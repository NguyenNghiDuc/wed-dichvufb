package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.catalog.ServiceCatalogAudit;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCatalogHistoryService {
    private final EntityManager em;
    public AdminCatalogHistoryService(EntityManager em) { this.em = em; }
    public record Result(List<ServiceCatalogAudit> events, boolean hasNext, int page) {}

    /** Exact, parameterized, read-only audit search. */
    @Transactional(readOnly = true)
    public Result page(int page, CampaignServiceType type, String actor) {
        if (page < 0 || page > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        String name = actor == null || actor.isBlank() ? null : actor.trim();
        if (name != null && !name.matches("[A-Za-z0-9._-]{3,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid actor");
        String query = "select a from ServiceCatalogAudit a where 1=1" +
            (type == null ? "" : " and a.serviceCode = :service") +
            (name == null ? "" : " and a.actor = :actor") +
            " order by a.createdAt desc, a.id desc";
        var q = em.createQuery(query, ServiceCatalogAudit.class);
        if (type != null) q.setParameter("service", type.name());
        if (name != null) q.setParameter("actor", name);
        var rows = q.setFirstResult(page * 20).setMaxResults(21).getResultList();
        return new Result(List.copyOf(rows.subList(0, Math.min(20, rows.size()))),
            rows.size() > 20, page);
    }
}
