package org.code.api.dto.attachment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttachmentCreateRequestDTO(
    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must be at most 255 characters")
    String fileName,
    @NotBlank(message = "File type is required")
    @Size(max = 50, message = "File type must be at most 50 characters")
    String fileType,
    @NotBlank(message = "Storage URL is required")
    String storageUrl
) {}
