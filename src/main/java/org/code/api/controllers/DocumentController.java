package org.code.api.controllers;

import java.util.UUID;

import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.services.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Exposes the existing attachment routes while the ownership/DTO refactor is pending.
 * The legacy multipart field name is preserved for the current wire contract.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    @Autowired
    private DocumentService documentService;

    /**
     * Accepts an attachment using the legacy multipart contract.
     * @param arquivo uploaded bytes under the existing documento field
     * @param request request containing the authenticated session
     * @return the current upload response
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocumento(@RequestParam("documento") MultipartFile arquivo, HttpServletRequest request) {
        
        try {
            Session session = (Session) request.getAttribute("session");
        
            UUID creatorId = session.getId(); // Authenticated user identity

         // Associate the upload with the authenticated creator
            Attachment docSalvo = documentService.registerDocument(arquivo, creatorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(docSalvo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Falha ao salvar anexo no servidor de arquivos: " + e.getMessage());
        }
    }

	// Attachment download
/**
 * Returns the stored bytes for the requested attachment.
 * @param id attachment identifier
 * @return the current download response
 */
@GetMapping("/{id}/download")
public ResponseEntity<byte[]> downloadDocumento(@PathVariable UUID id) {
    try {
        Attachment doc = documentService.findById(id);
        byte[] arquivoBytes = documentService.findLocalArchives(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType())) // Use the recorded media type
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(arquivoBytes);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
}

// Attachment deletion
/**
 * Removes an attachment through the existing service lifecycle.
 * @param id attachment identifier
 * @return the current deletion response
 */
@DeleteMapping("/{id}")
public ResponseEntity<String> deletarDocumento(@PathVariable UUID id) {
    try {
        documentService.deleteDocument(id);
        return ResponseEntity.ok("Documento e arquivo físico removidos com sucesso.");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
}
