package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.pressing.PressedBale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório para {@link PressedBale}. Sem creator_id — fardos são filhos de Pressing.
 */
@Repository
public interface PressedBaleRepository extends JpaRepository<PressedBale, UUID> {
    List<PressedBale> findAllByPressingId(UUID pressingId);
}
