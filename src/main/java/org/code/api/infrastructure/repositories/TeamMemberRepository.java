package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.base.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for TeamMember.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
  Optional<TeamMember> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<TeamMember> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
