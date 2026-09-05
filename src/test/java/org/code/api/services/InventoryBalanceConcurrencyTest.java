package org.code.api.services;

import org.code.api.domain.models.inventory.InventoryBalance;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.infrastructure.repositories.InventoryBalanceRepository;
import org.code.api.infrastructure.repositories.MaterialSubtypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that {@code @Version} on {@link InventoryBalance} actually prevents
 * lost updates. The deterministic part is {@link #forceCollision(UUID)}: two
 * reads of the same row, two writes — the second write must throw
 * {@link OptimisticLockingFailureException}. The multi-threaded part is
 * best-effort and may pass without collision depending on scheduling.
 */
@SpringBootTest
class InventoryBalanceConcurrencyTest {

    @Autowired
    private InventoryBalanceRepository balanceRepository;

    @Autowired
    private MaterialSubtypeRepository subtypeRepository;

    @Test
    void concurrent_updates_to_same_balance_should_result_in_optimistic_lock_exception() throws Exception {
        MaterialSubtype subtype = subtypeRepository.findAll().get(0);
        InventoryBalance balance = balanceRepository.findByMaterialSubtypeId(subtype.getId())
            .orElseGet(() -> balanceRepository.save(InventoryBalance.builder()
                .materialSubtype(subtype)
                .currentWeightKg(BigDecimal.ZERO)
                .currentVolumeM3(BigDecimal.ZERO)
                .build()));

        UUID balanceId = balance.getId();
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Void> update = () -> {
            start.await();
            for (int i = 0; i < 5; i++) {
                try {
                    updateBalanceAtomically(balanceId, BigDecimal.ONE);
                } catch (OptimisticLockingFailureException expected) {
                    return null;
                }
            }
            return null;
        };

        Future<Void> f1 = exec.submit(update);
        Future<Void> f2 = exec.submit(update);
        start.countDown();
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        exec.shutdown();

        assertThatThrownBy(() -> forceCollision(balanceId))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Transactional
    void updateBalanceAtomically(UUID id, BigDecimal delta) {
        InventoryBalance b = balanceRepository.findById(id).orElseThrow();
        b.setCurrentWeightKg(b.getCurrentWeightKg().add(delta));
        balanceRepository.saveAndFlush(b);
    }

    void forceCollision(UUID id) {
        InventoryBalance a = balanceRepository.findById(id).orElseThrow();
        InventoryBalance b = balanceRepository.findById(id).orElseThrow();
        a.setCurrentWeightKg(a.getCurrentWeightKg().add(BigDecimal.ONE));
        balanceRepository.saveAndFlush(a);
        b.setCurrentWeightKg(b.getCurrentWeightKg().add(BigDecimal.ONE));
        balanceRepository.saveAndFlush(b);
    }
}