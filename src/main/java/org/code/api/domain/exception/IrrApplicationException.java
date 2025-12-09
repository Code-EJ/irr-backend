package org.code.api.domain.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IrrApplicationException {

  private String service;
  private String message;
  private Throwable throwable;

  public IrrApplicationException(String service, String message) {
    this.service = service;
    this.message = message;
  }

  public IrrApplicationException(
    String service,
    String message,
    Throwable throwable
  ) {
    this.service = service;
    this.message = message;
    this.throwable = throwable;
  }
}
