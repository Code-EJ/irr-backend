package org.code.api.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.exception.DonationError;
import org.code.api.domain.exception.DonorError;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.models.base.Donor;
import org.code.api.domain.models.collection.InputItem;
import org.code.api.domain.models.donation.Donation;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.domain.ports.DonationPort;
import org.code.api.dto.collection.request.InputItemRequestDTO;
import org.code.api.dto.collection.response.InputItemResponseDTO;
import org.code.api.dto.donation.request.DonationCreateRequestDTO;
import org.code.api.dto.donation.request.DonationUpdateRequestDTO;
import org.code.api.dto.donation.response.DonationResponseDTO;
import org.code.api.infrastructure.repositories.AttachmentRepository;
import org.code.api.infrastructure.repositories.DonationRepository;
import org.code.api.infrastructure.repositories.DonorRepository;
import org.code.api.infrastructure.repositories.InputItemRepository;
import org.code.api.infrastructure.repositories.MaterialSubtypeRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela orquestração das regras de negócio relacionadas à entidade {@link Donation}.
 *
 * <p>Todas as operações filtram pelo {@code creator_id} do usuário autenticado,
 * garantindo isolamento de dados multilocatário.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService implements DonationPort {

    private final DonationRepository donationRepository;
    private final DonorRepository donorRepository;
    private final InputItemRepository inputItemRepository;
    private final MaterialSubtypeRepository materialSubtypeRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider userProvider;

    @Override
    @Transactional
    public DonationResponseDTO create(DonationCreateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();
        User creator = userRepository.getReferenceById(userId);

        if (data.inputItems() == null || data.inputItems().isEmpty()) {
            throw new DonationError.EmptyInputItems();
        }

        Donor donor = donorRepository.findByIdAndCreatorId(data.donorId(), userId)
                .orElseThrow(() -> new DonorError.NotFound(data.donorId()));

        Attachment attachment = resolveAttachment(data.proofAttachmentId(), userId);

        Donation donation = donationRepository.saveAndFlush(
                Donation.builder()
                        .donationDate(data.donationDate() != null ? data.donationDate() : OffsetDateTime.now())
                        .totalWeightKg(data.totalWeightKg())
                        .donor(donor)
                        .proofAttachment(attachment)
                        .isActive(true)
                        .creator(creator)
                        .build()
        );

        List<InputItem> items = buildInputItems(donation, data.inputItems(), userId);
        List<InputItem> savedItems = inputItemRepository.saveAllAndFlush(items);

        return toResponse(donation, savedItems);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationResponseDTO getById(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        Donation donation = donationRepository.findByIdAndCreatorId(id, userId)
                .orElseThrow(() -> new DonationError.NotFound(id));

        List<InputItem> items = inputItemRepository.findAllByDonationId(id);
        return toResponse(donation, items);
    }

    @Override
    @Transactional
    public DonationResponseDTO update(UUID id, DonationUpdateRequestDTO data) {
        UUID userId = userProvider.getCurrentUserId();

        Donation donation = donationRepository.findByIdAndCreatorId(id, userId)
                .orElseThrow(() -> new DonationError.NotFound(id));

        if (!donation.getIsActive()) {
            throw new DonationError.InactiveDonation(id);
        }

        if (data.inputItems() == null || data.inputItems().isEmpty()) {
            throw new DonationError.EmptyInputItems();
        }

        Attachment attachment = resolveAttachment(data.proofAttachmentId(), userId);

        donation.setDonationDate(data.donationDate() != null ? data.donationDate() : donation.getDonationDate());
        donation.setTotalWeightKg(data.totalWeightKg());
        donation.setProofAttachment(attachment);
        donationRepository.saveAndFlush(donation);

        // Substitui os itens: desativa os antigos e cria os novos
        List<InputItem> oldItems = inputItemRepository.findAllByDonationId(id);
        oldItems.forEach(item -> item.setIsActive(false));
        inputItemRepository.saveAll(oldItems);

        List<InputItem> newItems = buildInputItems(donation, data.inputItems(), userId);
        List<InputItem> savedItems = inputItemRepository.saveAllAndFlush(newItems);

        return toResponse(donation, savedItems);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        UUID userId = userProvider.getCurrentUserId();

        Donation donation = donationRepository.findByIdAndCreatorId(id, userId)
                .orElseThrow(() -> new DonationError.NotFound(id));

        if (!donation.getIsActive()) {
            throw new DonationError.InactiveDonation(id);
        }

        donation.setIsActive(false);
        donationRepository.save(donation);

        List<InputItem> items = inputItemRepository.findAllByDonationId(id);
        items.forEach(item -> item.setIsActive(false));
        inputItemRepository.saveAll(items);

        log.info("Donation {} deactivated", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DonationResponseDTO> list(Pageable pageable) {
        UUID userId = userProvider.getCurrentUserId();

        return donationRepository.findAllByCreatorId(userId, pageable)
                .map(donation -> toResponse(donation, inputItemRepository.findAllByDonationId(donation.getId())));
    }

    // ── Métodos privados ─────────────────────────────────────────────────────

    private Attachment resolveAttachment(UUID attachmentId, UUID userId) {
        if (attachmentId == null) {
            return null;
        }
        return attachmentRepository.findByIdAndCreatorId(attachmentId, userId)
                .orElseThrow(() -> new DonationError.AttachmentNotFound(attachmentId));
    }

    private List<InputItem> buildInputItems(Donation donation, List<InputItemRequestDTO> itemsData, UUID userId) {
        return itemsData.stream()
                .map(itemData -> {
                    MaterialSubtype subtype = materialSubtypeRepository
                            .findByIdAndCreatorId(itemData.materialSubtypeId(), userId)
                            .orElseThrow(() -> new MaterialError.NotFound(itemData.materialSubtypeId(), "SUBTYPE"));

                    return InputItem.builder()
                            .donation(donation)
                            .materialSubtype(subtype)
                            .weightKg(itemData.weightKg())
                            .volumeM3(itemData.volumeM3())
                            .isActive(true)
                            .build();
                })
                .toList();
    }

    private DonationResponseDTO toResponse(Donation donation, List<InputItem> items) {
        return new DonationResponseDTO(
                donation.getId(),
                donation.getDonationDate(),
                donation.getTotalWeightKg(),
                donation.getDonor().getId(),
                donation.getProofAttachment() != null ? donation.getProofAttachment().getId() : null,
                donation.getIsActive(),
                items.stream().map(this::toItemResponse).toList(),
                donation.getCreatedAt(),
                donation.getUpdatedAt()
        );
    }

    private InputItemResponseDTO toItemResponse(InputItem item) {
        return new InputItemResponseDTO(
                item.getId(),
                null,
                item.getDonation().getId(),
                item.getMaterialSubtype().getId(),
                item.getWeightKg(),
                item.getVolumeM3(),
                item.getIsActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}