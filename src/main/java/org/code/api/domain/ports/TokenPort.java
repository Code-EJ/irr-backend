package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

/**
 * Token Port boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface TokenPort {
  String createToken(Session session);

  Session decodeToken(String token);

  String renewToken(String token);

  String renewToken(Session session);
}
