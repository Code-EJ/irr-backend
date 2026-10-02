package org.code.api.domain.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * Groups domain failures for material operations and HTTP error translation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public class MaterialError extends RuntimeException {

  public MaterialError(String message) {
    super(message);
  }

  /** Conceals absent or foreign records behind one resource-not-found failure. */
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

  /** Indicates a duplicate name within the organization and material parent scope. */
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

  /** Rejects deactivation of a material with retained inventory references. */
  @Getter
  public static class HasInventoryBinding extends MaterialError {
    private final UUID materialId;
    private final String level;

    public HasInventoryBinding(UUID materialId, String level) {
      super(
          String.format(
              "Cannot delete material %s (%s): it has inventory records. Please contact the"
                  + " administrator.",
              materialId, level));
      this.materialId = materialId;
      this.level = level;
    }
  }

  /** Rejects mutation of an inactive material. */
  @Getter
  public static class InactiveMaterial extends MaterialError {
    private final UUID materialId;
    private final String level;

    public InactiveMaterial(UUID materialId, String level) {
      super(
          String.format("Material %s (%s) is inactive and cannot be modified.", materialId, level));
      this.materialId = materialId;
      this.level = level;
    }
  }

  /** Rejects a stale optimistic version; the client must reload the current representation. */
  @Getter
  public static class ConcurrentModification extends MaterialError {
    private final UUID materialId;

    public ConcurrentModification(UUID materialId) {
      super(
          String.format(
              "Material %s was modified by another user. Please refresh and try again.",
              materialId));
      this.materialId = materialId;
    }
  }

  /** Rejects a missing or foreign parent in the selected organization. */
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
