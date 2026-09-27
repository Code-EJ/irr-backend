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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates organization-owned intake without crediting saleable stock. Consumed input history
 * cannot be replaced or deactivated.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DonationService implements DonationPort {

  private final org.springframework.jdbc.core.JdbcTemplate jdbc;
  private final DonationRepository donationRepository;
  private final DonorRepository donorRepository;
  private final InputItemRepository inputItemRepository;
  private final MaterialSubtypeRepository materialSubtypeRepository;
  private final AttachmentRepository attachmentRepository;
  private final UserRepository userRepository;
  private final AuthenticatedUserProvider userProvider;
  private final org.code.api.domain.ports.OrganizationScope scope;

  @Override
  @Transactional
  public DonationResponseDTO create(DonationCreateRequestDTO data) {
    UUID organizationId = scope.organizationId();
    User creator = userRepository.getReferenceById(userProvider.getCurrentUserId());

    if (data.inputItems() == null || data.inputItems().isEmpty()) {
      throw new DonationError.EmptyInputItems();
    }

    Donor donor =
        donorRepository
            .findByIdAndOrganizationId(data.donorId(), organizationId)
            .orElseThrow(() -> new DonorError.NotFound(data.donorId()));

    Attachment attachment = resolveAttachment(data.proofAttachmentId(), organizationId);

    Donation donation =
        donationRepository.saveAndFlush(
            Donation.builder()
                .donationDate(
                    data.donationDate() != null ? data.donationDate() : OffsetDateTime.now())
                .totalWeightKg(total(data.inputItems(), data.totalWeightKg()))
                .donor(donor)
                .proofAttachment(attachment)
                .isActive(true)
                .creator(creator)
                .organizationId(organizationId)
                .build());

    List<InputItem> items = buildInputItems(donation, data.inputItems(), organizationId);
    List<InputItem> savedItems = inputItemRepository.saveAllAndFlush(items);

    return toResponse(donation, savedItems);
  }

  @Override
  @Transactional(readOnly = true)
  public DonationResponseDTO getById(UUID id) {
    UUID organizationId = scope.organizationId();

    Donation donation =
        donationRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new DonationError.NotFound(id));

    List<InputItem> items = inputItemRepository.findAllByDonationId(id);
    return toResponse(donation, items);
  }

  @Override
  @Transactional
  public DonationResponseDTO update(UUID id, DonationUpdateRequestDTO data) {
    UUID organizationId = scope.organizationId();

    Donation donation =
        donationRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new DonationError.NotFound(id));

    requireUnprocessed(id, organizationId);
    if (!donation.getIsActive()) {
      throw new DonationError.InactiveDonation(id);
    }

    if (data.inputItems() == null || data.inputItems().isEmpty()) {
      throw new DonationError.EmptyInputItems();
    }

    Attachment attachment = resolveAttachment(data.proofAttachmentId(), organizationId);

    donation.setDonationDate(
        data.donationDate() != null ? data.donationDate() : donation.getDonationDate());
    donation.setTotalWeightKg(total(data.inputItems(), data.totalWeightKg()));
    donation.setProofAttachment(attachment);
    donationRepository.saveAndFlush(donation);

    // Replace unprocessed items while retaining previous rows for audit.
    List<InputItem> oldItems = inputItemRepository.findAllByDonationId(id);
    oldItems.forEach(item -> item.setIsActive(false));
    inputItemRepository.saveAll(oldItems);

    List<InputItem> newItems = buildInputItems(donation, data.inputItems(), organizationId);
    List<InputItem> savedItems = inputItemRepository.saveAllAndFlush(newItems);

    return toResponse(donation, savedItems);
  }

  @Override
  @Transactional
  public void deactivate(UUID id) {
    UUID organizationId = scope.organizationId();

    Donation donation =
        donationRepository
            .findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new DonationError.NotFound(id));

    requireUnprocessed(id, organizationId);
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
    UUID organizationId = scope.organizationId();

    return donationRepository
        .findAllByOrganizationId(organizationId, pageable)
        .map(
            donation ->
                toResponse(donation, inputItemRepository.findAllByDonationId(donation.getId())));
  }

  // Internal mapping and validation.

  /** Preserves the source history once any sorting has consumed an intake item. */
  private void requireUnprocessed(UUID id, UUID organizationId) {
    Boolean used =
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM sorted_item s JOIN input_item i ON i.id=s.input_item_id"
                + " WHERE i.donation_id=? AND i.organization_id=?)",
            Boolean.class,
            id,
            organizationId);
    if (Boolean.TRUE.equals(used))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "Processed donations cannot be edited or deactivated");
  }

  /** Derives the total from line quantities and rejects inconsistent client totals. */
  private java.math.BigDecimal total(
      List<InputItemRequestDTO> items, java.math.BigDecimal declared) {
    java.math.BigDecimal total =
        items.stream()
            .map(InputItemRequestDTO::weightKg)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    if (declared != null && declared.compareTo(total) != 0)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "Total weight must equal the sum of input items");
    return total;
  }

  private Attachment resolveAttachment(UUID attachmentId, UUID organizationId) {
    if (attachmentId == null) {
      return null;
    }
    return attachmentRepository
        .findByIdAndOrganizationId(attachmentId, organizationId)
        .orElseThrow(() -> new DonationError.AttachmentNotFound(attachmentId));
  }

  private List<InputItem> buildInputItems(
      Donation donation, List<InputItemRequestDTO> itemsData, UUID organizationId) {
    return itemsData.stream()
        .map(
            itemData -> {
              MaterialSubtype subtype =
                  materialSubtypeRepository
                      .findByIdAndOrganizationId(itemData.materialSubtypeId(), organizationId)
                      .orElseThrow(
                          () ->
                              new MaterialError.NotFound(itemData.materialSubtypeId(), "SUBTYPE"));

              return InputItem.builder()
                  .donation(donation)
                  .materialSubtype(subtype)
                  .organizationId(organizationId)
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
        donation.getUpdatedAt());
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
        item.getUpdatedAt());
  }
}
