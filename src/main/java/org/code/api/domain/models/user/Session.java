package org.code.api.domain.models.user;

import java.time.Instant;
import java.util.UUID;

import org.code.api.domain.enums.UserType;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class Session {
  private UUID id;
  private String email;
  private UserType tipo;
  private long issuedAt;
  private long expiresAt;

  public boolean isExpired() {
    return Instant.now().toEpochMilli() > this.expiresAt;
  }

  public boolean isOnRenewalGrace() {
    if (isExpired()) {
      long now = Instant.now().toEpochMilli();
      long renewalWindowEnd = this.expiresAt + 24 * 60 * 60 * 1000; // 24 hours after expiration
    
      return now <= renewalWindowEnd;
    }

    return true;
  }
}
