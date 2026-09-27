package org.code.api.inventory.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.OrganizationScope;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sole stock posting boundary: immutable movements and the constrained projection commit
 * atomically. Organization write serialization and row predicates prevent overselling and negative
 * balances.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class StockLedger {
  private final JdbcTemplate jdbc;
  private final OrganizationScope scope;
  private final AuthenticatedUserProvider actor;

  /**
   * @param jdbc transaction-aware PostgreSQL access
   * @param scope verified organization
   * @param actor authenticated audit identity
   */
  public StockLedger(JdbcTemplate jdbc, OrganizationScope scope, AuthenticatedUserProvider actor) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.actor = actor;
  }

  /**
   * Starts a typed posting tied to one persisted operational document.
   *
   * @param kind SORTING, PRESSING or SALE
   * @param source source document identity
   * @param occurredAt business timestamp
   * @return immutable operation identity
   */
  public UUID begin(String kind, UUID source, OffsetDateTime occurredAt) {
    if (!java.util.Set.of("SORTING", "PRESSING", "SALE").contains(kind))
      throw new IllegalArgumentException("Unsupported stock operation");
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " stock_operation(id,organization_id,actor_id,kind,sorting_id,pressing_id,sale_id,occurred_at)"
            + " VALUES (?,?,?,?,?,?,?,?)",
        id,
        scope.organizationId(),
        actor.getCurrentUserId(),
        kind,
        kind.equals("SORTING") ? source : null,
        kind.equals("PRESSING") ? source : null,
        kind.equals("SALE") ? source : null,
        occurredAt);
    return id;
  }

  /**
   * Appends a movement and adjusts its material projection without clamping values.
   *
   * @param operation current operation identity
   * @param material scoped material subtype
   * @param weight signed mass delta
   * @param volume signed volume delta
   */
  public void move(UUID operation, UUID material, BigDecimal weight, BigDecimal volume) {
    UUID org = scope.organizationId();
    jdbc.update(
        "INSERT INTO"
            + " inventory_balance(id,organization_id,material_subtype_id,current_weight_kg,current_volume_m3)"
            + " VALUES (?,?,?,0,0) ON CONFLICT(material_subtype_id) DO NOTHING",
        UUID.randomUUID(),
        org,
        material);
    int changed =
        jdbc.update(
            "UPDATE inventory_balance SET"
                + " current_weight_kg=current_weight_kg+?,current_volume_m3=current_volume_m3+?,version=version+1,last_updated_at=CURRENT_TIMESTAMP"
                + " WHERE organization_id=? AND material_subtype_id=? AND current_weight_kg+?>=0"
                + " AND current_volume_m3+?>=0",
            weight,
            volume,
            org,
            material,
            weight,
            volume);
    if (changed != 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Insufficient stock for this operation");
    jdbc.update(
        "INSERT INTO"
            + " stock_movement(id,organization_id,operation_id,material_subtype_id,weight_delta_kg,volume_delta_m3)"
            + " VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        org,
        operation,
        material,
        weight,
        volume);
  }

  /**
   * Creates traceable saleable stock after sorting or pressing.
   *
   * @param material scoped subtype
   * @param sortedItem sorting output, or null for a pressed bale
   * @param bale pressed output, or null for a sorted item
   * @param weight available mass
   * @param volume available volume
   * @return lot identity
   */
  public UUID lot(UUID material, UUID sortedItem, UUID bale, BigDecimal weight, BigDecimal volume) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " stock_lot(id,organization_id,material_subtype_id,sorted_item_id,pressed_bale_id,available_weight_kg,available_volume_m3,original_weight_kg,original_volume_m3)"
            + " VALUES (?,?,?,?,?,?,?,?,?)",
        id,
        scope.organizationId(),
        material,
        sortedItem,
        bale,
        weight,
        volume,
        weight,
        volume);
    return id;
  }

  /**
   * Consumes a lot without allowing either physical quantity to become negative.
   *
   * @param lot lot identity
   * @param material expected material
   * @param weight requested mass
   * @param volume requested volume
   */
  public void consume(UUID lot, UUID material, BigDecimal weight, BigDecimal volume) {
    if (weight.signum() <= 0 || volume.signum() < 0)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Consumption must have positive mass and nonnegative volume");
    int changed =
        jdbc.update(
            "UPDATE stock_lot SET"
                + " available_weight_kg=available_weight_kg-?,available_volume_m3=available_volume_m3-?"
                + " WHERE organization_id=? AND id=? AND material_subtype_id=? AND is_active AND"
                + " available_weight_kg>=? AND available_volume_m3>=?",
            weight,
            volume,
            scope.organizationId(),
            lot,
            material,
            weight,
            volume);
    if (changed != 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Source lot is unavailable or has insufficient material");
  }

  /**
   * Reverses signed movements while preserving the original operation. Domain callers must first
   * prove no dependent allocations would be invalidated.
   *
   * @param kind original typed document kind
   * @param source original document identity
   * @return reversal operation identity
   */
  public UUID reverse(String kind, UUID source) {
    String column =
        switch (kind) {
          case "SORTING" -> "sorting_id";
          case "PRESSING" -> "pressing_id";
          case "SALE" -> "sale_id";
          default -> throw new IllegalArgumentException("Unsupported reversal");
        };
    UUID org = scope.organizationId();
    var existing =
        jdbc.queryForList(
            "SELECT id FROM stock_operation WHERE organization_id=? AND " + column + "=?",
            UUID.class,
            org,
            source);
    if (existing.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Posted operation not found");
    UUID original = existing.getFirst(), id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO stock_operation(id,organization_id,actor_id,kind,reversal_of,occurred_at)"
            + " VALUES (?,?,?,'REVERSAL',?,CURRENT_TIMESTAMP)",
        id,
        org,
        actor.getCurrentUserId(),
        original);
    var movements =
        jdbc.queryForList(
            "SELECT material_subtype_id,weight_delta_kg,volume_delta_m3 FROM stock_movement WHERE"
                + " organization_id=? AND operation_id=?",
            org,
            original);
    for (var row : movements)
      move(
          id,
          (UUID) row.get("material_subtype_id"),
          ((BigDecimal) row.get("weight_delta_kg")).negate(),
          ((BigDecimal) row.get("volume_delta_m3")).negate());
    return id;
  }
}
