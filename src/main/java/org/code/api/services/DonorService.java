package org.code.api.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.exception.DonorError;
import org.code.api.domain.models.base.Donor;
import org.code.api.domain.enums.DonorType;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.DonorPort;
import org.code.api.dto.donor.request.DonorCreateRequestDTO;
import org.code.api.dto.donor.request.DonorUpdateRequestDTO;
import org.code.api.dto.donor.response.DonorResponseDTO;
import org.code.api.infrastructure.repositories.DonorRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains organization-owned donor records with manager permissions and retained historical references.
  * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DonorService implements DonorPort {
    private final ReferenceLifecycleGuard lifecycle;

    private final DonorRepository donorRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;
    private final org.code.api.domain.ports.OrganizationScope scope;

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public DonorResponseDTO create(DonorCreateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");
        User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

        String document = normalizeDocument(data.document());
        validateDocument(document, data.donorType());

        if (donorRepository.existsByDocumentAndOrganizationId(document, organizationId)) {
            throw new DonorError.DocumentAlreadyExists(document);
        }

        try {
            Donor donor = donorRepository.saveAndFlush(
                    Donor.builder()
                            .name(data.name().trim())
                            .document(document)
                            .donorType(data.donorType())
                            .address(data.address()==null?null:data.address().toValue())
                            .isActive(true)
                            .creator(creator).organizationId(organizationId)
                            .build()
            );

            return toResponse(donor);
        } catch (DataIntegrityViolationException e) {
            throw new DonorError.DocumentAlreadyExists(document);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DonorResponseDTO getById(UUID id) {
        UUID organizationId = scope.organizationId();

        Donor donor = donorRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new DonorError.NotFound(id));

        return toResponse(donor);
    }

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public DonorResponseDTO update(UUID id, DonorUpdateRequestDTO data) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");

        Donor donor = donorRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new DonorError.NotFound(id));

        if (!donor.getIsActive()) {
            throw new DonorError.InactiveDonor(id);
        }

        String newDocument = normalizeDocument(data.document());
        validateDocument(newDocument, donor.getDonorType());

        if (!donor.getDocument().equals(newDocument) && donorRepository.existsByDocumentAndOrganizationId(newDocument, organizationId)) {
            throw new DonorError.DocumentAlreadyExists(newDocument);
        }

        donor.setName(data.name().trim());
        donor.setDocument(newDocument);
        donor.setAddress(data.address()==null?null:data.address().toValue());

        try {
            Donor updated = donorRepository.saveAndFlush(donor);
            return toResponse(updated);
        } catch (DataIntegrityViolationException e) {
            throw new DonorError.DocumentAlreadyExists(newDocument);
        }
    }

    @Override
    @PreAuthorize("@organizationScope.manager()")
    @Transactional
    public void deactivate(UUID id) {
        UUID organizationId = scope.organizationId();
        if (!scope.manager()) throw new org.springframework.security.access.AccessDeniedException("Organization manager permission is required");

        Donor donor = donorRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new DonorError.NotFound(id));
        lifecycle.donor(id);

        if (!donor.getIsActive()) {
            throw new DonorError.InactiveDonor(id);
        }

        donor.setIsActive(false);
        donorRepository.save(donor);

        log.info("Donor {} deactivated", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DonorResponseDTO> list(Pageable pageable) {
        UUID organizationId = scope.organizationId();
        return donorRepository.findAllByOrganizationId(organizationId, pageable).map(this::toResponse);
    }

    // ── Internal mapping and validation ─────────────────────────────────────────────────────

    private String normalizeDocument(String document) {
        return document.replaceAll("\\D", "");
    }

    private void validateDocument(String document, DonorType donorType) {
        if (donorType == DonorType.PF && document.length() != 11) {
            throw new IllegalArgumentException("CPF must have 11 digits");
        }
        if (donorType == DonorType.PJ && document.length() != 14) {
            throw new IllegalArgumentException("CNPJ must have 14 digits");
        }
    }

    private DonorResponseDTO toResponse(Donor donor) {
        return new DonorResponseDTO(
                donor.getId(),
                donor.getName(),
                donor.getDocument(),
                donor.getDonorType(),
                donor.getIsActive(),
                donor.getCreatedAt(),
                donor.getUpdatedAt(),
                org.code.api.dto.donor.PostalAddressDTO.from(donor.getAddress())
        );
    }
}