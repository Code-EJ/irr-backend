package org.code.api.domain.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Namespace centralizador das exceções de negócio da árvore tipológica de materiais.
 */
public class MaterialError extends RuntimeException {

    public MaterialError(String message) {
        super(message);
    }

    /**
     * Lançada quando nenhum material é encontrado com o ID informado.
     * Mapeada para HTTP 404.
     */
    @Getter
    public static class NotFound extends MaterialError {
        private final UUID materialId;
        private final String level;

        public NotFound(UUID materialId, String level) {
            super(String.format("Material %s not found at level: %s", materialId, level));
            this.materialId = materialId;
            this.level = level;
        }
    }

    /**
     * Lançada quando já existe um material com o mesmo nome no mesmo nível e tenant.
     * Mapeada para HTTP 409.
     */
    @Getter
    public static class NameAlreadyExists extends MaterialError {
        private final String name;
        private final String level;

        public NameAlreadyExists(String name, String level) {
            super(String.format("Material name '%s' already exists at level: %s", name, level));
            this.name = name;
            this.level = level;
        }
    }

    /**
     * Lançada quando um não-administrador tenta excluir um material que possui vínculos no estoque.
     * Mapeada para HTTP 409.
     */
    @Getter
    public static class HasInventoryBinding extends MaterialError {
        private final UUID materialId;
        private final String level;

        public HasInventoryBinding(UUID materialId, String level) {
            super(String.format(
                "Cannot delete material %s (%s): it has inventory records. Please contact the administrator.",
                materialId, level
            ));
            this.materialId = materialId;
            this.level = level;
        }
    }

    /**
     * Lançada ao tentar modificar um material que já está inativo.
     * Mapeada para HTTP 422.
     */
    @Getter
    public static class InactiveMaterial extends MaterialError {
        private final UUID materialId;
        private final String level;

        public InactiveMaterial(UUID materialId, String level) {
            super(String.format("Material %s (%s) is inactive and cannot be modified.", materialId, level));
            this.materialId = materialId;
            this.level = level;
        }
    }

    /**
     * Lançada quando ocorre conflito de versão (Optimistic Locking — RN01).
     * Mapeada para HTTP 409.
     */
    @Getter
    public static class ConcurrentModification extends MaterialError {
        private final UUID materialId;

        public ConcurrentModification(UUID materialId) {
            super(String.format(
                "Material %s was modified by another user. Please refresh and try again.", materialId
            ));
            this.materialId = materialId;
        }
    }

    /**
     * Lançada quando o material pai (Category ou Type) referenciado não existe ou não pertence ao usuário.
     * Mapeada para HTTP 404.
     */
    @Getter
    public static class ParentNotFound extends MaterialError {
        private final UUID parentId;
        private final String parentLevel;

        public ParentNotFound(UUID parentId, String parentLevel) {
            super(String.format("Parent material %s not found at level: %s", parentId, parentLevel));
            this.parentId = parentId;
            this.parentLevel = parentLevel;
        }
    }
}
