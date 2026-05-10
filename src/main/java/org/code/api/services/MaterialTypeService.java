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
import org.code.api.domain.ports.MaterialTypePort;
import org.code.api.dto.material.request.MaterialTypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialTypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialTypeResponseDTO;
import org.code.api.infrastructure.repositories.InventoryBalanceRepository;
import org.code.api.infrastructure.repositories.MaterialCategoryRepository;
import org.code.api.infrastructure.repositories.MaterialSubtypeRepository;
import org.code.api.infrastructure.repositories.MaterialTypeRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço para gestão de Tipos de Material (2° nível da árvore tipológica).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialTypeService implements MaterialTypePort {

    private static final String LEVEL = "TYPE";

    private final MaterialTypeRepository typeRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final MaterialSubtypeRepository subtypeRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    @Override
    @Transactional
    public MaterialTypeResponseDTO create(MaterialTypeCreateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();
        User creator = userRepository.getReferenceById(userId);

        // Validar que a categoria pai pertence ao mesmo creator
        MaterialCategory category = categoryRepository
            .findByIdAndCreatorId(data.categoryId(), userId)
            .orElseThrow(() -> new MaterialError.ParentNotFound(data.categoryId(), "CATEGORY"));

        if (!category.getIsActive()) {
            throw new MaterialError.InactiveMaterial(data.categoryId(), "CATEGORY");
        }

        String name = data.name().trim();

        if (typeRepository.existsByNameAndCategoryIdAndCreatorId(name, data.categoryId(), userId)) {
            throw new MaterialError.NameAlreadyExists(name, LEVEL);
        }

        MaterialType type = typeRepository.save(
            MaterialType.builder()
                .name(name)
                .category(category)
                .isActive(true)
                .creator(creator)
                .build()
        );

        return toResponse(type);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaterialTypeResponseDTO> list(UUID categoryId, String name, Pageable pageable) {
        UUID userId = userProvider.getCurrentUserId();

        Page<MaterialType> page;

        if (categoryId != null) {
            page = typeRepository.findAllByCategoryIdAndCreatorId(categoryId, userId, pageable);
        } else {
            page = typeRepository.findAllByCreatorId(userId, pageable);
        }

        if (name != null && !name.isBlank()) {
            String filter = name.trim().toUpperCase();
            return page.map(this::toResponse);
        }

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialTypeResponseDTO getById(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        MaterialType type = typeRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        return toResponse(type);
    }

    @Override
    @Transactional
    public MaterialTypeResponseDTO update(UUID id, MaterialTypeUpdateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();

        MaterialType type = typeRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        if (!type.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        type.setVersion(data.version());

        String newName = data.name().trim();

        if (!type.getName().equals(newName)
                && typeRepository.existsByNameAndCategoryIdAndCreatorId(
                    newName, type.getCategory().getId(), userId)) {
            throw new MaterialError.NameAlreadyExists(newName, LEVEL);
        }

        type.setName(newName);

        try {
            MaterialType updated = typeRepository.save(type);
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

        MaterialType type = typeRepository
            .findByIdAndCreatorId(id, userId)
            .orElseThrow(() -> new MaterialError.NotFound(id, LEVEL));

        if (!type.getIsActive()) {
            throw new MaterialError.InactiveMaterial(id, LEVEL);
        }

        // Verificar vínculos de estoque nos subtypes filhos
        List<MaterialSubtype> subtypes = subtypeRepository.findAllByTypeId(id);
        boolean hasInventoryBinding = subtypes.stream()
            .anyMatch(st -> inventoryBalanceRepository.existsByMaterialSubtypeId(st.getId()));

        if (hasInventoryBinding && !isAdmin) {
            throw new MaterialError.HasInventoryBinding(id, LEVEL);
        }

        // Cascata: inativar subtypes filhos
        for (MaterialSubtype subtype : subtypes) {
            subtype.setIsActive(false);
            subtypeRepository.save(subtype);
        }

        type.setIsActive(false);
        typeRepository.save(type);

        log.info("Material type {} deactivated with cascade ({} subtypes, admin={})", id, subtypes.size(), isAdmin);
    }

    private MaterialTypeResponseDTO toResponse(MaterialType type) {
        return new MaterialTypeResponseDTO(
            type.getId(),
            type.getCategory().getId(),
            type.getName(),
            type.getIsActive(),
            type.getVersion(),
            type.getCreatedAt(),
            type.getUpdatedAt()
        );
    }
}
