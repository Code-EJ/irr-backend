package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.material.MaterialSubtype;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialSubtypeRepository extends JpaRepository<MaterialSubtype, UUID> {
    Optional<MaterialSubtype> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<MaterialSubtype> findAllByCreatorId(UUID creatorId, Pageable pageable);
    Page<MaterialSubtype> findAllByTypeIdAndCreatorId(UUID typeId, UUID creatorId, Pageable pageable);
    boolean existsByNameAndTypeIdAndCreatorId(String name, UUID typeId, UUID creatorId);
    List<MaterialSubtype> findAllByTypeId(UUID typeId);
    List<MaterialSubtype> findAllByTypeIdIn(List<UUID> typeIds);
}
