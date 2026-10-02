package org.code.api.services;

import java.util.List;
import java.util.Objects;
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
import org.code.api.domain.ports.MaterialCategoryPort;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.dto.material.request.MaterialCategoryCreateRequestDTO;
import org.code.api.dto.material.request.MaterialCategoryUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialCategoryResponseDTO;
import org.code.api.infrastructure.repositories.InventoryBalanceRepository;
import org.code.api.infrastructure.repositories.MaterialCategoryRepository;
import org.code.api.infrastructure.repositories.MaterialSubtypeRepository;
import org.code.api.infrastructure.repositories.MaterialTypeRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.specifications.MaterialSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies creator-scoped material category use cases and server-managed optimistic locking. Catalog
 * sharing and organization membership are separate architectural decisions.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MaterialCategoryService implements MaterialCategoryPort {
  private final ReferenceLifecycleGuard lifecycle;

  private static final String LEVEL = "CATEGORY";

  private final MaterialCategoryRepository categoryRepository;
  private final MaterialTypeRepository typeRepository;
  private final MaterialSubtypeRepository subtypeRepository;
  private final InventoryBalanceRepository inventoryBalanceRepository;
  private final UserRepository userRepository;
  private final AuthenticatedUserProvider userProvider;
  private final OrganizationScope scope;

  /** {@inheritDoc} */
  @Override
  @PreAuthorize("@organizationScope.manager()")
  @Transactional
  public MaterialCategoryResponseDTO create(MaterialCategoryCreateRequestDTO data) {
    UUID organizationId = scope.organizationId();
    if (!scope.manager())
      throw new AccessDeniedException("Organization manager permission is required");
    User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

    String name = data.name().trim();

    if (categoryRepository.existsByNameAndOrganizationId(name, organizationId)) {
      throw new MaterialError.NameAlreadyExists(name, LEVEL);
    }

    MaterialCategory category =
        categoryRepository.save(
            MaterialCategory.builder()
                .name(name)
                .isActive(true)
                .creator(creator)
                .organizationId(organizationId)
                .build());

    return toResponse(category);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public Page<MaterialCategoryResponseDTO> list(String name, Pageable pageable) {
    UUID organizationId = scope.organizationId();
    return categoryRepository
        .findAll(MaterialSearch.matching(organizationId, null, null, name), pageable)
        .map(this::toResponse);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public MaterialCategoryResponseDTO getById(UUID id) {
    UUID organizationId = scope.organizationId();

    MaterialCategory category =
        categoryRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

    return toResponse(category);
  }

  /** {@inheritDoc} */
  @Override
  @PreAuthorize("@organizationScope.manager()")
  @Transactional
  public MaterialCategoryResponseDTO update(UUID id, MaterialCategoryUpdateRequestDTO data) {
    UUID organizationId = scope.organizationId();
    if (!scope.manager())
      throw new AccessDeniedException("Organization manager permission is required");

    MaterialCategory category =
        categoryRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

    if (!category.getIsActive()) {
      throw new MaterialError.InactiveMaterial(id, LEVEL);
    }

    // Compare the client version without modifying Hibernate-managed state.
    if (!Objects.equals(category.getVersion(), data.version())) {
      throw new MaterialError.ConcurrentModification(id);
    }

    String newName = data.name().trim();

    if (!category.getName().equals(newName)
        && categoryRepository.existsByNameAndOrganizationId(newName, organizationId)) {
      throw new MaterialError.NameAlreadyExists(newName, LEVEL);
    }

    category.setName(newName);

    try {
      MaterialCategory updated = categoryRepository.saveAndFlush(category);
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
      throw new AccessDeniedException("Organization manager permission is required");
    List<UserRole> roles = userProvider.getCurrentUserRoles();
    boolean isAdmin = roles.contains(UserRole.ADMINISTRATOR);

    MaterialCategory category =
        categoryRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));
    lifecycle.material(id, LEVEL);

    if (!category.getIsActive()) {
      throw new MaterialError.InactiveMaterial(id, LEVEL);
    }

    // Inspect inventory bindings through the category/type/subtype hierarchy.
    List<MaterialType> types = typeRepository.findAllByCategoryId(id);
    List<UUID> typeIds = types.stream().map(MaterialType::getId).toList();

    boolean hasInventoryBinding = false;
    if (!typeIds.isEmpty()) {
      List<MaterialSubtype> subtypes = subtypeRepository.findAllByTypeIdIn(typeIds);
      hasInventoryBinding =
          subtypes.stream()
              .anyMatch(st -> inventoryBalanceRepository.existsByMaterialSubtypeId(st.getId()));
    }

    if (hasInventoryBinding) {
      throw new MaterialError.HasInventoryBinding(id, LEVEL);
    }

    // Deactivate descendants using the existing lifecycle policy.
    for (MaterialType type : types) {
      List<MaterialSubtype> subtypes = subtypeRepository.findAllByTypeId(type.getId());
      for (MaterialSubtype subtype : subtypes) {
        subtype.setIsActive(false);
        subtypeRepository.save(subtype);
      }
      type.setIsActive(false);
      typeRepository.save(type);
    }

    category.setIsActive(false);
    categoryRepository.save(category);

    log.info(
        "Material category {} deactivated with cascade ({} types, admin={})",
        id,
        types.size(),
        isAdmin);
  }

  private MaterialCategoryResponseDTO toResponse(MaterialCategory category) {
    return new MaterialCategoryResponseDTO(
        category.getId(),
        category.getName(),
        category.getIsActive(),
        category.getVersion(),
        category.getCreatedAt(),
        category.getUpdatedAt());
  }
}
