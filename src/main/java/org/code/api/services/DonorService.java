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
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela orquestração das regras de negócio relacionadas à entidade {@link Donor}.
 *
 * <p>Todas as operações filtram pelo {@code creator_id} do usuário autenticado,
 * garantindo isolamento de dados multilocatário.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonorService implements DonorPort {

    private final DonorRepository donorRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    @Override
    @Transactional
    public DonorResponseDTO create(DonorCreateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();
        User creator = userRepository.getReferenceById(userId);

        String document = normalizeDocument(data.document());
        validateDocument(document, data.donorType());

        if (donorRepository.existsByDocument(document)) {
            throw new DonorError.DocumentAlreadyExists(document);
        }

        try {
            Donor donor = donorRepository.saveAndFlush(
                    Donor.builder()
                            .name(data.name().trim())
                            .document(document)
                            .donorType(data.donorType())
                            .isActive(true)
                            .creator(creator)
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
        UUID userId = userProvider.getCurrentUserId();

        Donor donor = donorRepository
                .findByIdAndCreatorId(id, userId)
                .orElseThrow(() -> new DonorError.NotFound(id));

        return toResponse(donor);
    }

    @Override
    @Transactional
    public DonorResponseDTO update(UUID id, DonorUpdateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();

        Donor donor = donorRepository
                .findByIdAndCreatorId(id, userId)
                .orElseThrow(() -> new DonorError.NotFound(id));

        if (!donor.getIsActive()) {
            throw new DonorError.InactiveDonor(id);
        }

        String newDocument = normalizeDocument(data.document());
        validateDocument(newDocument, donor.getDonorType());

        if (!donor.getDocument().equals(newDocument) && donorRepository.existsByDocument(newDocument)) {
            throw new DonorError.DocumentAlreadyExists(newDocument);
        }

        donor.setName(data.name().trim());
        donor.setDocument(newDocument);

        try {
            Donor updated = donorRepository.saveAndFlush(donor);
            return toResponse(updated);
        } catch (DataIntegrityViolationException e) {
            throw new DonorError.DocumentAlreadyExists(newDocument);
        }
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        Donor donor = donorRepository
                .findByIdAndCreatorId(id, userId)
                .orElseThrow(() -> new DonorError.NotFound(id));

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
        UUID userId = userProvider.getCurrentUserId();
        return donorRepository.findAllByCreatorId(userId, pageable).map(this::toResponse);
    }

    // ── Métodos privados ─────────────────────────────────────────────────────

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
                donor.getUpdatedAt()
        );
    }
}