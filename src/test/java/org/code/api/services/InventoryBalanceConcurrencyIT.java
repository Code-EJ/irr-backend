package org.code.api.services;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.code.api.domain.models.inventory.InventoryBalance;
import org.code.api.infrastructure.repositories.InventoryBalanceRepository;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checks independent-transaction stale writes and database inventory integrity.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class InventoryBalanceConcurrencyIT extends PostgresIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired InventoryBalanceRepository balances;
  @Autowired PlatformTransactionManager manager;

  /** Verifies that two competing first inserts cannot create duplicate material balances. */
  @Test
  void concurrentFirstInsertsCommitExactlyOneBalance() throws Exception {
    UUID subtype = fixture();
    var start = new java.util.concurrent.CountDownLatch(1);
    var ready = new java.util.concurrent.CountDownLatch(2);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Boolean> insert =
          () -> {
            ready.countDown();
            if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
              throw new IllegalStateException("Concurrent insert start timed out");
            }
            try {
              jdbc.update("INSERT INTO inventory_balance(material_subtype_id) VALUES (?)", subtype);
              return true;
            } catch (org.springframework.dao.DuplicateKeyException expected) {
              return false;
            }
          };
      var first = executor.submit(insert);
      var second = executor.submit(insert);
      assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(
              java.util.List.of(
                  first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(10, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM inventory_balance WHERE material_subtype_id = ?",
                Integer.class,
                subtype))
        .isEqualTo(1);
  }

  /** Verifies that stale independent transaction cannot overwrite committed balance. */
  @Test
  void staleIndependentTransactionCannotOverwriteCommittedBalance() {
    UUID subtype = fixture();
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO inventory_balance(id, material_subtype_id) VALUES (?, ?)", id, subtype);
    TransactionTemplate transaction = new TransactionTemplate(manager);
    InventoryBalance first = transaction.execute(status -> balances.findById(id).orElseThrow());
    InventoryBalance stale = transaction.execute(status -> balances.findById(id).orElseThrow());
    first.setCurrentWeightKg(BigDecimal.ONE);
    transaction.executeWithoutResult(status -> balances.saveAndFlush(first));
    stale.setCurrentWeightKg(BigDecimal.TEN);
    assertThatThrownBy(
            () -> transaction.executeWithoutResult(status -> balances.saveAndFlush(stale)))
        .isInstanceOf(OptimisticLockingFailureException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT current_weight_kg FROM inventory_balance WHERE id = ?",
                BigDecimal.class,
                id))
        .isEqualByComparingTo(BigDecimal.ONE);
  }

  /** Verifies that duplicate balance and negative quantity are rejected by database. */
  @Test
  void duplicateBalanceAndNegativeQuantityAreRejectedByDatabase() {
    UUID subtype = fixture();
    jdbc.update("INSERT INTO inventory_balance(material_subtype_id) VALUES (?)", subtype);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO inventory_balance(material_subtype_id) VALUES (?)", subtype))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE inventory_balance SET current_weight_kg = -1 WHERE material_subtype_id"
                        + " = ?",
                    subtype))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private UUID fixture() {
    UUID user = UUID.randomUUID(),
        category = UUID.randomUUID(),
        type = UUID.randomUUID(),
        subtype = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,user_role) VALUES"
            + " (?,?,'fixture','Fixture','ADMINISTRATOR')",
        user,
        user + "@example.test");
    jdbc.update(
        "INSERT INTO material_category(id,name,creator_id) VALUES (?,'Category',?)",
        category,
        user);
    jdbc.update(
        "INSERT INTO material_type(id,name,creator_id,category_id) VALUES (?,'Type',?,?)",
        type,
        user,
        category);
    jdbc.update(
        "INSERT INTO material_subtype(id,name,creator_id,type_id) VALUES (?,'Subtype',?,?)",
        subtype,
        user,
        type);
    return subtype;
  }
}
