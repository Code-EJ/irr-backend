package org.code.api.repository;


import org.code.api.domain.models.logistic.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Integer> {
    
    Optional<Driver> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
}
