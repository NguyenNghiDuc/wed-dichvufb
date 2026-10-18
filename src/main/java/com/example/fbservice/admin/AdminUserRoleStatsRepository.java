package com.example.fbservice.admin;

import com.example.fbservice.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** One grouped read, independent of the core authentication repository. */
public interface AdminUserRoleStatsRepository extends Repository<User, Long> {
    @Query("""
        select new com.example.fbservice.admin.AdminUserRoleCount(
          u.role, count(u.id),
          sum(case when u.enabled = true then 1L else 0L end))
        from User u group by u.role order by u.role
        """)
    List<AdminUserRoleCount> groupByRole();
}
