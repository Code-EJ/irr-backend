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

    Optional<MaterialCategory> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<MaterialCategory> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    boolean existsByNameAndOrganizationId(String name, UUID organizationId);
}
