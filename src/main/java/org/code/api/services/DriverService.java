package org.code.api.services;

import lombok.AllArgsConstructor;
import org.code.api.domain.models.logistic.Driver;
import org.code.api.domain.ports.DriverPort;
import org.code.api.infrastructure.repositories.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DriverService implements DriverPort {

  @Autowired
  private DriverRepository driverRepository;

  public Driver createDriver(String nome, String cpf) {
    try {
      Driver driver = driverRepository.save(
        Driver.builder().nome(nome).cpf(cpf).build()
      );

      return driver;
    } catch (Exception exception) {
      throw exception;
    }
  }
}
