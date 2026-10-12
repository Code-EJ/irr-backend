package org.code.api.infrastructure.repositories;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for User.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface UserRepository extends JpaRepository<User, UUID> {
  /** Serializes profile, administrative and credential changes on the same active identity. */
  @org.springframework.data.jpa.repository.Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> lockById(@Param("id") UUID id);

  @Query("SELECT u FROM User u WHERE u.email = :email")
  Optional<User> findByEmail(@Param("email") String email);

  @Query(
      "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
  boolean existsByEmail(@Param("email") String email);
}
