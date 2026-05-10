package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.material.MaterialCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, UUID>,
        JpaSpecificationExecutor<MaterialCategory> {

    Optional<MaterialCategory> findByIdAndCreatorId(UUID id, UUID creatorId);

    Page<MaterialCategory> findAllByCreatorId(UUID creatorId, Pageable pageable);

    boolean existsByNameAndCreatorId(String name, UUID creatorId);
}
