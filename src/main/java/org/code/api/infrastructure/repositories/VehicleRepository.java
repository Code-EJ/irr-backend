package org.code.api.infrastructure.repositories;

import java.util.Optional;
import org.code.api.domain.models.logistic.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    boolean existsByPlaca(String placa);

    Optional<Vehicle> findByPlaca(String placa);
}
