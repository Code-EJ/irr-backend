package org.code.api.services;
import java.util.UUID;
import org.code.api.domain.ports.OrganizationScope;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;
/**
 * Preserves historical references before soft deletion hides related entities from JPA reads.
 * The organization write lock makes checks atomic with operational creation and deactivation.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service @lombok.RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class ReferenceLifecycleGuard {
    private final JdbcTemplate jdbc;
    private final OrganizationScope scope;
    /** Rejects vehicle deactivation while any collection retains its identity. */
    public void vehicle(UUID id) { reject("SELECT EXISTS(SELECT 1 FROM collection WHERE vehicle_id=? AND organization_id=?)",id); }
    /** Rejects donor deactivation while any donation retains its identity. */
    public void donor(UUID id) { reject("SELECT EXISTS(SELECT 1 FROM donation WHERE donor_id=? AND organization_id=?)",id); }
    /** Rejects material hierarchy deactivation if a descendant has any operational history. */
    public void material(UUID id,String level) {
        String predicate=switch(level) { case "CATEGORY" -> "t.category_id"; case "TYPE" -> "s.type_id"; case "SUBTYPE" -> "s.id"; default -> throw new IllegalArgumentException("Unknown material level"); };
        reject("SELECT EXISTS(SELECT 1 FROM material_subtype s JOIN material_type t ON t.id=s.type_id WHERE "+predicate+"=? AND s.organization_id=? AND (EXISTS(SELECT 1 FROM input_item WHERE material_subtype_id=s.id) OR EXISTS(SELECT 1 FROM sorted_item WHERE material_subtype_id=s.id) OR EXISTS(SELECT 1 FROM pressed_bale WHERE material_subtype_id=s.id) OR EXISTS(SELECT 1 FROM sale_item WHERE material_subtype_id=s.id) OR EXISTS(SELECT 1 FROM inventory_balance WHERE material_subtype_id=s.id)))",id);
    }
    private void reject(String sql,UUID id) {
        if(Boolean.TRUE.equals(jdbc.queryForObject(sql,Boolean.class,id,scope.organizationId())))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Referenced records must remain available for operational history");
    }
}
