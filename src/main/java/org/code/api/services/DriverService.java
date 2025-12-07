package org.code.api.services;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.code.api.domain.models.logistic.Driver;
import org.code.api.infrastructure.repositories.DriverRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DriverService {

  // private DriverRepository driverRepository;

  public Optional<Driver> createDriver() {
    return Optional.empty();
  }
}
