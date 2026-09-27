package org.code.api.services;

import java.io.IOException;
import java.util.UUID;
import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.dto.attachment.AttachmentResponse;
import org.code.api.infrastructure.repositories.AttachmentRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

/**
 * Enforces organization membership and coordinates transactional metadata with persistent bytes.
 * Upload rollback cleanup is compensating; process crashes still require orphan reconciliation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Service
public class DocumentService {
    private final AttachmentRepository attachments;
    private final UserRepository users;
    private final StorageService storage;
    private final AuthenticatedUserProvider actor;
    private final AttachmentCleanupService cleanup;
    private final org.code.api.domain.ports.OrganizationScope scope;
    /**
     * Configures attachment boundaries.
     * @param attachments metadata repository
     * @param users active creator repository
     * @param storage byte storage
     * @param actor authenticated identity
     * @param cleanup durable deletion queue
     */
    public DocumentService(AttachmentRepository attachments, UserRepository users, StorageService storage,
        AuthenticatedUserProvider actor, AttachmentCleanupService cleanup, org.code.api.domain.ports.OrganizationScope scope) {
        this.attachments = attachments; this.users = users; this.storage = storage; this.actor = actor; this.cleanup = cleanup; this.scope = scope;
    }
    /**
     * Validates supported signatures and persists safe upload metadata.
     * @param file PDF, PNG or JPEG content, at most ten MiB
     * @return safe attachment metadata
     * @throws IOException if persistent storage fails
     */
    @Transactional(rollbackFor = IOException.class)
    public AttachmentResponse upload(MultipartFile file) throws IOException {
        UUID organizationId=scope.organizationId();
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw badRequest("Attachment must contain between 1 byte and 10 MiB");
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || name.length() > 255 || name.chars().anyMatch(Character::isISOControl)
            || name.indexOf('/') >= 0 || name.indexOf((char) 92) >= 0) throw badRequest("A valid display filename is required");
        byte[] header;
        try (var input = file.getInputStream()) { header = input.readNBytes(8); }
        String contentType = detect(header);
        var creator = users.findById(actor.getCurrentUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String path = storage.store(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { storage.delete(path); }
                    catch (RuntimeException exception) { log.error("Upload rollback left an orphan; storage reconciliation is required"); }
                }
            }
        });
        Attachment attachment = new Attachment();
        attachment.setFileName(name); attachment.setFileType(contentType); attachment.setStorageUrl(path);
        attachment.setOrganizationId(organizationId); attachment.setCreator(creator); attachment.setIsActive(true);
        attachments.saveAndFlush(attachment);
        return new AttachmentResponse(attachment.getId(), name, contentType, attachment.getCreatedAt());
    }
    /**
     * Returns bytes to active organization members; platform administrators have no implicit access.
     * @param id attachment identity
     * @return safe download information
     * @throws IOException if bytes are unavailable
     */
    @Transactional(readOnly = true)
    public Download download(UUID id) throws IOException {
        Attachment attachment = owned(id);
        return new Download(attachment.getFileName(), attachment.getFileType(), storage.read(attachment.getStorageUrl()));
    }
    /**
     * Deletes unreferenced metadata and enqueues physical removal in the same transaction.
     * @param id attachment identity
     */
    @Transactional
    public void delete(UUID id) {
        Attachment attachment = owned(id);
        if(!attachment.getCreator().getId().equals(actor.getCurrentUserId()) && !scope.manager())
            throw new org.springframework.security.access.AccessDeniedException("Only the uploader or organization manager can delete an attachment");
        attachments.delete(attachment);
        attachments.flush();
        cleanup.enqueue(attachment.getStorageUrl());
    }
    /** Lists safe metadata visible to the selected organization. */
    @Transactional(readOnly=true)
    public org.springframework.data.domain.Page<AttachmentResponse> list(org.springframework.data.domain.Pageable page) {
        return attachments.findAllByOrganizationId(scope.organizationId(),page).map(this::metadata);
    }
    /** Returns one scoped metadata record without storage paths. */
    @Transactional(readOnly=true)
    public AttachmentResponse get(UUID id) { return metadata(owned(id)); }
    /** Renames display metadata without changing stored bytes or their detected content type. */
    @Transactional
    public AttachmentResponse rename(UUID id,String name) {
        Attachment attachment=owned(id);
        if(!attachment.getCreator().getId().equals(actor.getCurrentUserId()) && !scope.manager())
            throw new org.springframework.security.access.AccessDeniedException("Only the uploader or organization manager can rename an attachment");
        if(name==null || name.isBlank() || name.length()>255 || name.chars().anyMatch(Character::isISOControl) || name.indexOf('/')>=0 || name.indexOf((char)92)>=0)
            throw badRequest("A valid display filename is required");
        attachment.setFileName(name.strip()); attachments.saveAndFlush(attachment); return metadata(attachment);
    }
    private AttachmentResponse metadata(Attachment attachment) { return new AttachmentResponse(attachment.getId(),attachment.getFileName(),attachment.getFileType(),attachment.getCreatedAt()); }
    private Attachment owned(UUID id) {
        return attachments.findByIdAndOrganizationId(id, scope.organizationId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
    }
    private String detect(byte[] header) {
        if (header.length >= 5 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F' && header[4] == '-') return "application/pdf";
        if (header.length >= 8 && java.util.Arrays.equals(header, new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (header.length >= 3 && header[0] == (byte)255 && header[1] == (byte)216 && header[2] == (byte)255) return "image/jpeg";
        throw badRequest("Only PDF, PNG and JPEG signatures are accepted");
    }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    /**
     * Internal download value; never serialized as a JSON persistence entity.
     * @param fileName display filename
     * @param contentType recorded media type
     * @param bytes bounded content
     */
    public record Download(String fileName, String contentType, byte[] bytes) {}
}
