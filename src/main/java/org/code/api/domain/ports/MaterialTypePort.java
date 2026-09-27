package org.code.api.domain.ports;

import org.code.api.dto.material.request.MaterialTypeCreateRequestDTO;
import org.code.api.dto.material.request.MaterialTypeUpdateRequestDTO;
import org.code.api.dto.material.response.MaterialTypeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Defines creator-scoped material type operations. Writes require an administrator.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface MaterialTypePort {

    /**
     * Creates a material in the authenticated creator scope.
     * @param data validated creation fields
     * @return created material metadata
     */
    MaterialTypeResponseDTO create(MaterialTypeCreateRequestDTO data);

    /**
     * Filters in the database before pagination and counting.
     * @param categoryId optional creator-owned parent filter
     * @param name optional case-insensitive literal substring
     * @param pageable page and sort request
     * @return matching active materials in the authenticated creator scope
     */
    Page<MaterialTypeResponseDTO> list(UUID categoryId, String name, Pageable pageable);

    /**
     * Reads an active material in the authenticated creator scope.
     * @param id material identity
     * @return owned material metadata
     */
    MaterialTypeResponseDTO getById(UUID id);

    /**
     * Updates only when the client version matches the persisted version.
     * @param id material identity
     * @param data new fields and expected version
     * @return flushed metadata including the current server-managed version
     * @throws org.code.api.domain.exception.MaterialError.ConcurrentModification if the expected version is stale
     */
    MaterialTypeResponseDTO update(UUID id, MaterialTypeUpdateRequestDTO data);

    /**
     * Applies the existing creator-scoped deactivation policy.
     * @param id material identity
     */
    void deactivate(UUID id);
}
