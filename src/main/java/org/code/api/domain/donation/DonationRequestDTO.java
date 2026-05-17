package org.code.api.domain.donation;

public record DonationRequestDTO(
        String donorName,
        String donorDocument,
        String donorType,
        String donorAddress,
        Long materialTypeId,
        Long materialSubtypeId,
        Long materialSubSubtypeId,
        Double weight
) {
}
