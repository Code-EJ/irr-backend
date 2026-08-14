package org.code.api.repositories;

import org.code.api.domain.prensagem.Prensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrensagemRepository extends JpaRepository<Prensagem, UUID> {
    List<Prensagem> findBySubtipologiaId(UUID subtipologiaId);
}
