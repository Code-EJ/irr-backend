package org.code.api.domain.models.user;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class Session {

  private UUID id;
  private String email;
  private long issuedAt;
  private long expiresAt;
}
