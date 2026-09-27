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

    private static final String LEVEL = "SUBTYPE";

    private final MaterialSubtypeRepository subtypeRepository;
    private final MaterialTypeRepository typeRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public MaterialSubtypeResponseDTO create(MaterialSubtypeCreateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();
        User creator = userRepository.getReferenceById(userId);

        MaterialType type = typeRepository
            .findByIdAndCreatorId(data.typeId(), userId)
            .orElseThrow(() -> new MaterialError.ParentNotFound(data.typeId(), "TYPE"));

        if (!type.getIsActive()) {
            throw new MaterialError.InactiveMaterial(data.typeId(), "TYPE");
        }

        String name = data.name().trim();

        if (subtypeRepository.existsByNameAndTypeIdAndCreatorId(name, data.typeId(), userId)) {
            throw new MaterialError.NameAlreadyExists(name, LEVEL);
        }

        MaterialSubtype subtype = subtypeRepository.save(
            MaterialSubtype.builder()
                .name(name)
                .type(type)
                .isActive(true)
                .creator(creator)
                .build()
        );

        return toResponse(subtype);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<MaterialSubtypeResponseDTO> list(UUID typeId, String name, Pageable pageable) {
        UUID userId = userProvider.getCurrentUserId();
        return subtypeRepository.findAll(MaterialSearch.matching(userId, "type", typeId, name), pageable)
            .map(this::toResponse);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public MaterialSubtypeResponseDTO getById(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        MaterialSubtype subtype = subtypeRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        return toResponse(subtype);
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public MaterialSubtypeResponseDTO update(UUID id, MaterialSubtypeUpdateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();

        MaterialSubtype subtype = subtypeRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        if (!subtype.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        if (!java.util.Objects.equals(subtype.getVersion(), data.version())) {
            throw new MaterialError.ConcurrentModification(id);
        }

        String newName = data.name().trim();

        if (!subtype.getName().equals(newName)
                && subtypeRepository.existsByNameAndTypeIdAndCreatorId(
                    newName, subtype.getType().getId(), userId)) {
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
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public void deactivate(UUID id) {
        UUID userId = userProvider.getCurrentUserId();
        List<UserRole> roles = userProvider.getCurrentUserRoles();
        boolean isAdmin = roles.contains(UserRole.ADMINISTRATOR);

        MaterialSubtype subtype = subtypeRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        if (!subtype.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        // Inspect the direct inventory binding.
        boolean hasInventoryBinding = inventoryBalanceRepository.existsByMaterialSubtypeId(id);

        if (hasInventoryBinding && !isAdmin) {
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
