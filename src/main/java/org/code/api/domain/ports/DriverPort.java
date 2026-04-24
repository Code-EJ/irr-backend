package org.code.api.domain.ports;

import org.code.api.domain.models.logistic.Driver;

public interface DriverPort {
  Driver createDriver(String nome, String cpf);
}
