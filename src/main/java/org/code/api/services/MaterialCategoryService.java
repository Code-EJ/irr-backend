package org.code.api.services;

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
import org.code.api.dto.material.request.MaterialCategoryCreateRequestDTO;
import org.code.api.dto.material.request.MaterialCategoryUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialCategoryResponseDTO;
import org.code.api.infrastructure.repositories.InventoryBalanceRepository;
import org.code.api.infrastructure.repositories.MaterialCategoryRepository;
import org.code.api.infrastructure.repositories.MaterialSubtypeRepository;
import org.code.api.infrastructure.repositories.MaterialTypeRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.infrastructure.specifications.MaterialCategorySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço para gestão de Categorias de Material (1° nível da árvore tipológica).
 *
 * <p>Regras de negócio:</p>
 * <ul>
 *   <li>Unicidade de nome por tenant (creator_id)</li>
 *   <li>Optimistic locking via @Version</li>
 *   <li>Deactivate: admin pode desativar com cascata; não-admin bloqueado se houver vínculo de estoque</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialCategoryService implements MaterialCategoryPort {

    private static final String LEVEL = "CATEGORY";

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialTypeRepository typeRepository;
    private final MaterialSubtypeRepository subtypeRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    @Override
    @Transactional
    public MaterialCategoryResponseDTO create(MaterialCategoryCreateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();
        User creator = userRepository.getReferenceById(userId);

        String name = data.name().trim();

        if (categoryRepository.existsByNameAndCreatorId(name, userId)) {
            throw new MaterialError.NameAlreadyExists(name, LEVEL);
        }

        MaterialCategory category = categoryRepository.save(
            MaterialCategory.builder()
                .name(name)
                .isActive(true)
                .creator(creator)
                .build()
        );

        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaterialCategoryResponseDTO> list(String name, Pageable pageable) {
        UUID userId = userProvider.getCurrentUserId();

        Specification<MaterialCategory> spec = MaterialCategorySpecification.withCreatorId(userId);

        if (name != null && !name.isBlank()) {
            spec = spec.and(MaterialCategorySpecification.nameContains(name));
        }

        return categoryRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialCategoryResponseDTO getById(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        MaterialCategory category = categoryRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        return toResponse(category);
    }

    @Override
    @Transactional
    public MaterialCategoryResponseDTO update(UUID id, MaterialCategoryUpdateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();

        MaterialCategory category = categoryRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        if (!category.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        // Optimistic lock: setar a version do DTO na entidade antes do save
        category.setVersion(data.version());

        String newName = data.name().trim();

        if (!category.getName().equals(newName)
                && categoryRepository.existsByNameAndCreatorId(newName, userId)) {
            throw new MaterialError.NameAlreadyExists(newName, LEVEL);
        }

        category.setName(newName);

        try {
            MaterialCategory updated = categoryRepository.save(category);
            return toResponse(updated);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new MaterialError.ConcurrentModification(id);
        }
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        UUID userId = userProvider.getCurrentUserId();
        List<UserRole> roles = userProvider.getCurrentUserRoles();
        boolean isAdmin = roles.contains(UserRole.ADMINISTRATOR);

        MaterialCategory category = categoryRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        if (!category.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        // Verificar vínculos de estoque em cascata: Category → Types → Subtypes → InventoryBalance
        List<MaterialType> types = typeRepository.findAllByCategoryId(id);
        List<UUID> typeIds = types.stream().map(MaterialType::getId).toList();

        boolean hasInventoryBinding = false;
        if (!typeIds.isEmpty()) {
            List<MaterialSubtype> subtypes = subtypeRepository.findAllByTypeIdIn(typeIds);
            hasInventoryBinding = subtypes.stream()
                .anyMatch(st -> inventoryBalanceRepository.existsByMaterialSubtypeId(st.getId()));
        }

        if (hasInventoryBinding && !isAdmin) {
            throw new MaterialError.HasInventoryBinding(id, LEVEL);
        }

        // Cascata de inativação: Category → Types → Subtypes
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

        log.info("Material category {} deactivated with cascade ({} types, admin={})", id, types.size(), isAdmin);
    }

    private MaterialCategoryResponseDTO toResponse(MaterialCategory category) {
        return new MaterialCategoryResponseDTO(
            category.getId(),
            category.getName(),
            category.getIsActive(),
            category.getVersion(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
