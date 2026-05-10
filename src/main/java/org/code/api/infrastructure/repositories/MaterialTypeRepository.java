package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.material.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialTypeRepository extends JpaRepository<MaterialType, UUID> {
    Optional<MaterialType> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<MaterialType> findAllByCreatorId(UUID creatorId, Pageable pageable);
    Page<MaterialType> findAllByCategoryIdAndCreatorId(UUID categoryId, UUID creatorId, Pageable pageable);
    boolean existsByNameAndCategoryIdAndCreatorId(String name, UUID categoryId, UUID creatorId);
    List<MaterialType> findAllByCategoryId(UUID categoryId);
}
