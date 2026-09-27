package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.base.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para a entidade {@link Vehicle}.
 *
 * <p>Estende {@link JpaSpecificationExecutor} para suportar filtragem dinâmica
 * via Criteria API (Specifications).</p>
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID>,
        JpaSpecificationExecutor<Vehicle> {

    boolean existsByLicensePlateAndOrganizationId(String licensePlate, UUID organizationId);

    Optional<Vehicle> findByLicensePlateAndOrganizationId(String licensePlate, UUID organizationId);

    Optional<Vehicle> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Vehicle> findAllByOrganizationId(UUID organizationId, Pageable pageable);

}
