package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.base.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    Optional<TeamMember> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<TeamMember> findAllByCreatorId(UUID creatorId, Pageable pageable);
}
