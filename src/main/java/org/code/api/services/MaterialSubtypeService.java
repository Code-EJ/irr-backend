package org.code.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.material.MaterialType;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.MaterialSubtypePort;
import org.code.api.dto.material.request.MaterialSubtypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialSubtypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialSubtypeResponseDTO;
import org.code.api.infrastructure.repositories.InventoryBalanceRepository;
import org.code.api.infrastructure.repositories.MaterialSubtypeRepository;
import org.code.api.infrastructure.repositories.MaterialTypeRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.code.api.infrastructure.specifications.MaterialSearch;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Applies creator-scoped material subtype use cases and server-managed optimistic locking.
 * Catalog sharing and organization membership are separate architectural decisions.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MaterialSubtypeService implements MaterialSubtypePort {
    private final ReferenceLifecycleGuard lifecycle;

    private static final String LEVEL = "SUBTYPE";

    private final MaterialSubtypeRepository subtypeRepository;
    private final MaterialTypeRepository typeRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;
    private final org.code.api.domain.ports.OrganizationScope scope;

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public MaterialSubtypeResponseDTO create(MaterialSubtypeCreateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");
        User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

        MaterialType type = typeRepository
            .findByIdAndOrganizationId(data.typeId(), organizationId)
            .orElseThrow(() -> new MaterialError.ParentNotFound(data.typeId(), "TYPE"));

        if (!type.getIsActive()) {
            throw new MaterialError.InactiveMaterial(data.typeId(), "TYPE");
        }

        String name = data.name().trim();

        if (subtypeRepository.existsByNameAndTypeIdAndOrganizationId(name, data.typeId(), organizationId)) {
            throw new MaterialError.NameAlreadyExists(name, LEVEL);
        }

        MaterialSubtype subtype = subtypeRepository.save(
            MaterialSubtype.builder()
                .name(name)
                .type(type)
                .isActive(true)
                .creator(creator).organizationId(organizationId)
                .build()
        );

        return toResponse(subtype);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<MaterialSubtypeResponseDTO> list(UUID typeId, String name, Pageable pageable) {
        UUID organizationId = scope.organizationId();
        return subtypeRepository.findAll(MaterialSearch.matching(organizationId, "type", typeId, name), pageable)
            .map(this::toResponse);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public MaterialSubtypeResponseDTO getById(UUID id) {
        UUID organizationId = scope.organizationId();

        MaterialSubtype subtype = subtypeRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        return toResponse(subtype);
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public MaterialSubtypeResponseDTO update(UUID id, MaterialSubtypeUpdateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");

        MaterialSubtype subtype = subtypeRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        if (!subtype.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        if (!java.util.Objects.equals(subtype.getVersion(), data.version())) {
            throw new MaterialError.ConcurrentModification(id);
        }

        String newName = data.name().trim();

        if (!subtype.getName().equals(newName)
                && subtypeRepository.existsByNameAndTypeIdAndOrganizationId(
                    newName, subtype.getType().getId(), organizationId)) {
            throw new MaterialError.NameAlreadyExists(newName, LEVEL);
        }

        subtype.setName(newName);

        try {
            MaterialSubtype updated = subtypeRepository.saveAndFlush(subtype);
            return toResponse(updated);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new MaterialError.ConcurrentModification(id);
        }
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public void deactivate(UUID id) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");
        List<UserRole> roles = userProvider.getCurrentUserRoles();
        boolean isAdmin = roles.contains(UserRole.ADMINISTRATOR);

        MaterialSubtype subtype = subtypeRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));
        lifecycle.material(id, LEVEL);

        if (!subtype.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        // Inspect the direct inventory binding.
        boolean hasInventoryBinding = inventoryBalanceRepository.existsByMaterialSubtypeId(id);

        if (hasInventoryBinding) {
            throw new MaterialError.HasInventoryBinding(id, LEVEL);
        }

        subtype.setIsActive(false);
        subtypeRepository.save(subtype);

        log.info("Material subtype {} deactivated (admin={}, had_inventory={})", id, isAdmin, hasInventoryBinding);
    }

    private MaterialSubtypeResponseDTO toResponse(MaterialSubtype subtype) {
        return new MaterialSubtypeResponseDTO(
            subtype.getId(),
            subtype.getType().getId(),
            subtype.getName(),
            subtype.getIsActive(),
            subtype.getVersion(),
            subtype.getCreatedAt(),
            subtype.getUpdatedAt()
        );
    }
}
