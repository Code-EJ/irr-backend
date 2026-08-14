package org.code.api.repositories;

import org.code.api.domain.tipologia.Tipologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TipologiaRepository extends JpaRepository<Tipologia, UUID> {
}
