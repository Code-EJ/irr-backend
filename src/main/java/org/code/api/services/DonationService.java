package org.code.api.services;

import lombok.RequiredArgsConstructor;
import org.code.api.domain.donation.Donation;
import org.code.api.domain.donation.DonationRequestDTO;
import org.code.api.domain.donation.DonationResponseDTO;
import org.code.api.repositories.DonationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository repository;

    public DonationResponseDTO create(DonationRequestDTO dto) {
        validate(dto);

        Donation donation = new Donation();
                donation.setDonorName(dto.donorName());
                donation.setDonorDocument(cleanDocument(dto.donorDocument()));
                donation.setDonorType(dto.donorType());
                donation.setDonorAddress(dto.donorAddress());
                donation.setMaterialTypeId(dto.materialTypeId());
                donation.setMaterialSubtypeId(dto.materialSubtypeId());
                donation.setMaterialSubSubtypeId(dto.materialSubSubtypeId());
                donation.setWeight(dto.weight());

        return mapToResponse(repository.save(donation));
    }

    public List<DonationResponseDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DonationResponseDTO findById(Long id) {
        Donation donation = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));

        return mapToResponse(donation);
    }

    public DonationResponseDTO update(Long id, DonationRequestDTO dto) {
        validate(dto);

        Donation donation = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found"));

        donation.setDonorName(dto.donorName());
        donation.setDonorDocument(cleanDocument(dto.donorDocument()));
        donation.setDonorType(dto.donorType());
        donation.setDonorAddress(dto.donorAddress());
        donation.setMaterialTypeId(dto.materialTypeId());
        donation.setMaterialSubtypeId(dto.materialSubtypeId());
        donation.setMaterialSubSubtypeId(dto.materialSubSubtypeId());
        donation.setWeight(dto.weight());

        return mapToResponse(repository.save(donation));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private DonationResponseDTO mapToResponse(Donation donation) {
        return new DonationResponseDTO(
                donation.getId(),
                donation.getDonorName(),
                donation.getDonorDocument(),
                donation.getDonorType(),
                donation.getDonorAddress(),
                donation.getMaterialTypeId(),
                donation.getMaterialSubtypeId(),
                donation.getMaterialSubSubtypeId(),
                donation.getWeight()
        );
    }

    private void validate(DonationRequestDTO dto) {
        if (dto.donorName() == null || dto.donorName().isBlank()) {
            throw new RuntimeException("Donor name is required");
        }

        if (dto.donorDocument() == null || dto.donorDocument().isBlank()) {
            throw new RuntimeException("Donor document is required");
        }

        if (dto.donorType() == null || (!dto.donorType().equals("PF") && !dto.donorType().equals("PJ"))) {
            throw new RuntimeException("Donor type must be PF or PJ");
        }

        if (dto.donorAddress() == null || dto.donorAddress().isBlank()) {
            throw new RuntimeException("Donor address is required");
        }

        String cleanDocument = cleanDocument(dto.donorDocument());

        if (dto.donorType().equals("PF") && cleanDocument.length() != 11) {
            throw new RuntimeException("CPF must have 11 digits");
        }

        if (dto.donorType().equals("PJ") && cleanDocument.length() != 14) {
            throw new RuntimeException("CNPJ must have 14 digits");
        }

        if (dto.materialTypeId() == null) {
            throw new RuntimeException("Material type is required");
        }

        if (dto.materialSubtypeId() == null) {
            throw new RuntimeException("Material subtype is required");
        }

        if (dto.materialSubSubtypeId() == null) {
            throw new RuntimeException("Material sub subtype is required");
        }

        if (dto.weight() == null || dto.weight() <= 0) {
            throw new RuntimeException("Weight must be greater than zero");
        }
    }

    private String cleanDocument(String document) {
        return document.replaceAll("\\D", "");
    }
}
