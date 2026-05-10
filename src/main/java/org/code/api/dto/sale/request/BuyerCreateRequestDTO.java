package org.code.api.dto.sale.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuyerCreateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @Size(max = 20, message = "Document must be at most 20 characters")
    String document
) {}
