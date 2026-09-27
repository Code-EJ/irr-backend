package org.code.api.services;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.models.material.MaterialCategory;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.material.MaterialType;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.MaterialTypePort;
import org.code.api.dto.material.request.MaterialTypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialTypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialTypeResponseDTO;
import org.code.api.infrastructure.repositories.InventoryBalanceRepository;
import org.code.api.infrastructure.repositories.MaterialCategoryRepository;
import org.code.api.infrastructure.repositories.MaterialSubtypeRepository;
import org.code.api.infrastructure.repositories.MaterialTypeRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.specifications.MaterialSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies creator-scoped material type use cases and server-managed optimistic locking. Catalog
 * sharing and organization membership are separate architectural decisions.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MaterialTypeService implements MaterialTypePort {
  private final ReferenceLifecycleGuard lifecycle;

  private static final String LEVEL = "TYPE";

  private final MaterialTypeRepository typeRepository;
  private final MaterialCategoryRepository categoryRepository;
  private final MaterialSubtypeRepository subtypeRepository;
  private final InventoryBalanceRepository inventoryBalanceRepository;
  private final UserRepository userRepository;
  private final AuthenticatedUserProvider userProvider;
  private final org.code.api.domain.ports.OrganizationScope scope;

  /** {@inheritDoc} */
  @Override
  @PreAuthorize("@organizationScope.manager()")
  @Transactional
  public MaterialTypeResponseDTO create(MaterialTypeCreateRequestDTO data) {
    UUID organizationId = scope.organizationId();
    if (!scope.manager())
      throw new org.springframework.security.access.AccessDeniedException(
          "Organization manager permission is required");
    User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

    // Validate that the parent category belongs to the authenticated creator.
    MaterialCategory category =
        categoryRepository
            .findByIdAndOrganizationId(data.categoryId(), organizationId)
            .orElseThrow(() -> new MaterialError.ParentNotFound(data.categoryId(), "CATEGORY"));

    if (!category.getIsActive()) {
      throw new MaterialError.InactiveMaterial(data.categoryId(), "CATEGORY");
    }

    String name = data.name().trim();

    if (typeRepository.existsByNameAndCategoryIdAndOrganizationId(
        name, data.categoryId(), organizationId)) {
      throw new MaterialError.NameAlreadyExists(name, LEVEL);
    }

    MaterialType type =
        typeRepository.save(
            MaterialType.builder()
                .name(name)
                .category(category)
                .isActive(true)
                .creator(creator)
                .organizationId(organizationId)
                .build());

    return toResponse(type);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public Page<MaterialTypeResponseDTO> list(UUID categoryId, String name, Pageable pageable) {
    UUID organizationId = scope.organizationId();
    return typeRepository
        .findAll(MaterialSearch.matching(organizationId, "category", categoryId, name), pageable)
        .map(this::toResponse);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public MaterialTypeResponseDTO getById(UUID id) {
    UUID organizationId = scope.organizationId();

    MaterialType type =
        typeRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

    return toResponse(type);
  }

  /** {@inheritDoc} */
  @Override
  @PreAuthorize("@organizationScope.manager()")
  @Transactional
  public MaterialTypeResponseDTO update(UUID id, MaterialTypeUpdateRequestDTO data) {
    UUID organizationId = scope.organizationId();
    if (!scope.manager())
      throw new org.springframework.security.access.AccessDeniedException(
          "Organization manager permission is required");

    MaterialType type =
        typeRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

    if (!type.getIsActive()) {
      throw new MaterialError.InactiveMaterial(id, LEVEL);
    }

    if (!java.util.Objects.equals(type.getVersion(), data.version())) {
      throw new MaterialError.ConcurrentModification(id);
    }

    String newName = data.name().trim();

    if (!type.getName().equals(newName)
        && typeRepository.existsByNameAndCategoryIdAndOrganizationId(
            newName, type.getCategory().getId(), organizationId)) {
      throw new MaterialError.NameAlreadyExists(newName, LEVEL);
    }

    type.setName(newName);

    try {
      MaterialType updated = typeRepository.saveAndFlush(type);
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
    if (!scope.manager())
      throw new org.springframework.security.access.AccessDeniedException(
          "Organization manager permission is required");
    List<UserRole> roles = userProvider.getCurrentUserRoles();
    boolean isAdmin = roles.contains(UserRole.ADMINISTRATOR);

    MaterialType type =
        typeRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));
    lifecycle.material(id, LEVEL);

    if (!type.getIsActive()) {
      throw new MaterialError.InactiveMaterial(id, LEVEL);
    }

    // Inspect inventory bindings for child subtypes.
    List<MaterialSubtype> subtypes = subtypeRepository.findAllByTypeId(id);
    boolean hasInventoryBinding =
        subtypes.stream()
            .anyMatch(st -> inventoryBalanceRepository.existsByMaterialSubtypeId(st.getId()));

    if (hasInventoryBinding) {
      throw new MaterialError.HasInventoryBinding(id, LEVEL);
    }

    // Deactivate child subtypes.
    for (MaterialSubtype subtype : subtypes) {
      subtype.setIsActive(false);
      subtypeRepository.save(subtype);
    }

    type.setIsActive(false);
    typeRepository.save(type);

    log.info(
        "Material type {} deactivated with cascade ({} subtypes, admin={})",
        id,
        subtypes.size(),
        isAdmin);
  }

  private MaterialTypeResponseDTO toResponse(MaterialType type) {
    return new MaterialTypeResponseDTO(
        type.getId(),
        type.getCategory().getId(),
        type.getName(),
        type.getIsActive(),
        type.getVersion(),
        type.getCreatedAt(),
        type.getUpdatedAt());
  }
}
