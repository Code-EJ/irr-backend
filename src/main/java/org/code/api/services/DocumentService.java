package org.code.api.services;

import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.models.user.User;
import org.code.api.infrastructure.repositories.AttachmentRepository;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * Legacy attachment orchestration. Metadata authorization, response DTOs and
 * transactional file lifecycle are scheduled for the identity/attachment slice.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
public class DocumentService {

    @Autowired
    private AttachmentRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;



    /**
     * Stores uploaded bytes and persists their legacy metadata record.
     * @param file uploaded content
     * @param creatorId existing creator identity
     * @return persisted attachment metadata
     * @throws IOException if byte storage fails
     */
    @Transactional
    public Attachment registerDocument(MultipartFile file, UUID creatorId) throws IOException {
        String path = storageService.store(file);
        Attachment doc = new Attachment();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setStorageUrl(path);
        doc.setIsActive(true);
        Optional<User> optionalCreator = userRepository.findById(creatorId);
        if (optionalCreator.isPresent()) {
            User creator = optionalCreator.get();
            doc.setCreator(creator);
        }
        


        return repository.save(doc);
    }

    /**
     * Reads bytes for an attachment using the legacy metadata path.
     * @param id attachment identifier
     * @return stored bytes
     * @throws IOException if the file cannot be read
     */
    @Transactional(readOnly = true)
    public byte[] findLocalArchives(UUID id) throws IOException {
        Attachment doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No attachment exists for the supplied ID"));
        return Files.readAllBytes(Paths.get(doc.getStorageUrl()));
    }

    /**
     * Finds the current attachment entity; callers must not expose it as a new API contract.
     * @param id attachment identifier
     * @return attachment entity
     */
    public Attachment findById(UUID id) {
        Attachment doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No attachment exists for the supplied ID"));
        return doc;
    }
    /**
     * Deletes bytes and metadata through the legacy flow.
     * <p>This method still needs the planned durable deletion lifecycle before release.</p>
     * @param id attachment identifier
     */
    @Transactional
    public void deleteDocument(UUID id) {
        Attachment doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No attachment exists for the supplied ID"));

        try {
            Files.deleteIfExists(Paths.get(doc.getStorageUrl()));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao apagar o file físico do servidor mock", e);
        }
        repository.delete(doc);
    }
}