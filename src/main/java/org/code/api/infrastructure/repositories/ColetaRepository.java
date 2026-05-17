package org.code.api.infrastructure.repositories;

import org.code.api.domain.coleta.Coleta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColetaRepository extends JpaRepository<Coleta, Long> {
}