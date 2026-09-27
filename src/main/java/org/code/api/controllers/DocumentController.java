package org.code.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.code.api.dto.attachment.AttachmentResponse;
import org.code.api.services.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Exposes organization-scoped attachments while retaining the legacy multipart field name.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping({"/api/documents", "/api/v1/documents"})
public class DocumentController {
  private final DocumentService documents;

  /**
   * @param documents organization-scoped attachment use cases
   */
  public DocumentController(DocumentService documents) {
    this.documents = documents;
  }

  /**
   * Uploads an attachment for the current actor.
   *
   * @param file legacy documento multipart part
   * @return public metadata without storage paths or user credentials
   * @throws IOException if byte storage fails
   */
  @Operation(
      summary = "Upload an attachment",
      description =
          "Authenticated creators may upload PDF, PNG or JPEG content up to 10 MiB. Signature"
              + " checks are not malware scanning. The legacy multipart name documento is"
              + " retained.")
  @ApiResponse(responseCode = "201", description = "Safe attachment metadata")
  @PostMapping(
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AttachmentResponse> upload(@RequestParam("documento") MultipartFile file)
      throws IOException {
    return ResponseEntity.status(201).body(documents.upload(file));
  }

  /**
   * Downloads content belonging to the selected organization.
   *
   * @param id attachment identity
   * @return bounded bytes with a safe download filename
   * @throws IOException if bytes cannot be read
   */
  @Operation(
      summary = "Download an owned attachment",
      description =
          "Active organization members can download the attachment. Missing and foreign IDs both"
              + " return 404.")
  @GetMapping("/{id}/download")
  public ResponseEntity<byte[]> download(@PathVariable UUID id) throws IOException {

    var file = documents.download(id);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(file.bytes());
  }

  /**
   * Removes metadata and durably schedules physical deletion.
   *
   * @param id attachment identity
   * @return acceptance after the database transaction commits
   */
  @Operation(
      summary = "Delete an owned attachment",
      description =
          "The uploader or an organization manager may delete an unreferenced attachment."
              + " Referenced attachments return 409. Metadata removal commits before asynchronous"
              + " physical cleanup; failures are retried.")
  @ApiResponse(responseCode = "202", description = "Metadata removed and durable cleanup accepted")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {

    documents.delete(id);

    return ResponseEntity.accepted().build();
  }

  /** Lists safe attachment metadata for the selected organization. */
  @GetMapping
  public Page<AttachmentResponse> list(
      @PageableDefault(size = 20, sort = "createdAt") Pageable page) {
    return documents.list(page);
  }

  /** Reads one attachment metadata record. */
  @GetMapping("/{id}")
  public AttachmentResponse get(@PathVariable UUID id) {
    return documents.get(id);
  }

  /** Replaces the display filename without modifying evidence bytes. */
  @PutMapping("/{id}")
  public AttachmentResponse rename(
      @PathVariable UUID id, @jakarta.validation.Valid @RequestBody RenameAttachment request) {
    return documents.rename(id, request.fileName());
  }

  /** Validated display metadata for an existing attachment. */
  public record RenameAttachment(
      @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 255)
          String fileName) {}
}
