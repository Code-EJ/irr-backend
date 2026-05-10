package org.code.api.domain.models.user;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.code.api.domain.enums.UserRole;

@Builder
@Getter
@Setter
public class Session {

    private UUID id;
    private String email;
    private UserRole userRole;
    private Instant issuedAt;
    private Instant expiresAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isOnRenewalGrace() {
        if (isExpired()) {
            Instant now = Instant.now();
            Instant renewalWindowEnd = this.expiresAt.plusSeconds(24 * 60 * 60); // 24 hours after expiration

            return now.isBefore(renewalWindowEnd);
        }

        return true;
    }
}
