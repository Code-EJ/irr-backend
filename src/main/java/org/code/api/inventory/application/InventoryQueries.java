package org.code.api.inventory.application;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.inventory.api.InventoryContract.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads authoritative PostgreSQL quantities without stale authorization or stock caches.
 * Reconciliation uses one repeatable snapshot and never repairs data by overwriting history.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@lombok.RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Transactional(
    readOnly = true,
    isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class InventoryQueries {
  private final JdbcTemplate jdbc;
  private final OrganizationScope scope;

  /** Lists current balances in stable material-name order. */
  public Page<Balance> balances(Pageable page) {
    UUID org = scope.organizationId();
    var rows =
        jdbc.query(
            "SELECT b.*,m.name FROM inventory_balance b JOIN material_subtype m ON"
                + " m.id=b.material_subtype_id WHERE b.organization_id=? ORDER BY"
                + " m.name,b.material_subtype_id LIMIT ? OFFSET ?",
            (r, n) ->
                new Balance(
                    id(r, "material_subtype_id"),
                    r.getString("name"),
                    r.getBigDecimal("current_weight_kg"),
                    r.getBigDecimal("current_volume_m3"),
                    r.getLong("version")),
            org,
            page.getPageSize(),
            page.getOffset());
    return new PageImpl<>(rows, page, count("inventory_balance", org));
  }

  /** Lists traceable active lots, optionally restricted to one scoped material. */
  public Page<Lot> lots(UUID material, Pageable page) {
    UUID org = scope.organizationId();
    String filter = material == null ? "" : " AND l.material_subtype_id=?";
    var args = new java.util.ArrayList<Object>();
    args.add(org);
    if (material != null) args.add(material);
    long total =
        jdbc.queryForObject(
            "SELECT count(*) FROM stock_lot l WHERE l.is_active AND l.organization_id=?" + filter,
            Long.class,
            args.toArray());
    args.add(page.getPageSize());
    args.add(page.getOffset());
    var rows =
        jdbc.query(
            "SELECT l.*,m.name FROM stock_lot l JOIN material_subtype m ON"
                + " m.id=l.material_subtype_id WHERE l.is_active AND l.organization_id=?"
                + filter
                + " ORDER BY l.created_at,l.id LIMIT ? OFFSET ?",
            (r, n) ->
                new Lot(
                    id(r, "id"),
                    id(r, "material_subtype_id"),
                    r.getString("name"),
                    id(r, "sorted_item_id"),
                    id(r, "pressed_bale_id"),
                    r.getBigDecimal("available_weight_kg"),
                    r.getBigDecimal("available_volume_m3"),
                    r.getObject("created_at", OffsetDateTime.class)),
            args.toArray());
    return new PageImpl<>(rows, page, total);
  }

  /** Lists signed movements in reverse business-time order with stable identity tie-breaking. */
  public Page<Movement> movements(Pageable page) {
    UUID org = scope.organizationId();
    var rows =
        jdbc.query(
            "SELECT m.*,o.kind,o.occurred_at,o.actor_id FROM stock_movement m JOIN stock_operation"
                + " o ON o.id=m.operation_id WHERE m.organization_id=? ORDER BY o.occurred_at"
                + " DESC,m.id LIMIT ? OFFSET ?",
            (r, n) ->
                new Movement(
                    id(r, "id"),
                    id(r, "operation_id"),
                    r.getString("kind"),
                    id(r, "material_subtype_id"),
                    r.getBigDecimal("weight_delta_kg"),
                    r.getBigDecimal("volume_delta_m3"),
                    r.getObject("occurred_at", OffsetDateTime.class),
                    id(r, "actor_id")),
            org,
            page.getPageSize(),
            page.getOffset());
    return new PageImpl<>(rows, page, count("stock_movement", org));
  }

  /** Compares all three stock representations, including missing balance rows. */
  public List<Reconciliation> reconcile() {
    UUID org = scope.organizationId();
    return jdbc.query(
        "WITH movements AS (SELECT material_subtype_id,SUM(weight_delta_kg) w,SUM(volume_delta_m3)"
            + " v FROM stock_movement WHERE organization_id=? GROUP BY material_subtype_id), lots"
            + " AS (SELECT material_subtype_id,SUM(available_weight_kg) w,SUM(available_volume_m3)"
            + " v FROM stock_lot WHERE organization_id=? AND is_active GROUP BY"
            + " material_subtype_id) SELECT s.id,COALESCE(b.current_weight_kg,0)-COALESCE(m.w,0)"
            + " dw,COALESCE(b.current_volume_m3,0)-COALESCE(m.v,0)"
            + " dv,COALESCE(b.current_weight_kg,0)-COALESCE(l.w,0)"
            + " lw,COALESCE(b.current_volume_m3,0)-COALESCE(l.v,0) lv FROM material_subtype s LEFT"
            + " JOIN inventory_balance b ON b.material_subtype_id=s.id LEFT JOIN movements m ON"
            + " m.material_subtype_id=s.id LEFT JOIN lots l ON l.material_subtype_id=s.id WHERE"
            + " s.organization_id=? ORDER BY s.id",
        (r, n) -> {
          BigDecimal dw = r.getBigDecimal("dw"),
              dv = r.getBigDecimal("dv"),
              lw = r.getBigDecimal("lw"),
              lv = r.getBigDecimal("lv");
          return new Reconciliation(
              id(r, "id"),
              dw,
              dv,
              lw,
              lv,
              dw.signum() == 0 && dv.signum() == 0 && lw.signum() == 0 && lv.signum() == 0);
        },
        org,
        org,
        org);
  }

  private long count(String table, UUID org) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE organization_id=?", Long.class, org);
  }

  private UUID id(ResultSet r, String column) throws SQLException {
    return r.getObject(column, UUID.class);
  }
}
