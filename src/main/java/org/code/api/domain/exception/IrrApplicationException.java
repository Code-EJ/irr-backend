package org.code.api.domain.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Irr Application Exception boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Getter
@Setter
public class IrrApplicationException extends Error {

  private String service;
  private String message;
  private Throwable throwable;

  public IrrApplicationException(String service, String message) {
    super(String.format("%s - %s", service, message));
    this.service = service;
    this.message = message;
  }

  public IrrApplicationException(String service, String message, Throwable throwable) {
    this.service = service;
    this.message = message;
    this.throwable = throwable;
  }
}
