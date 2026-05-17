package org.code.api.controllers;

import java.util.UUID;

import org.code.api.domain.models.documents.DocumentoComprobatorio;
import org.code.api.services.DocumentoService;
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




@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    @Autowired
    private DocumentoService documentoService;
	
	@Transactional
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocumento(@RequestParam("documento") MultipartFile arquivo) {
        try {
            DocumentoComprobatorio docSalvo = documentoService.registrarDocumento(arquivo);
            return ResponseEntity.status(HttpStatus.CREATED).body(docSalvo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Falha ao salvar anexo no servidor de arquivos: " + e.getMessage());
        }
    }

	// Endpoint para Download Automático (GET)
@GetMapping("/{id}/download")
public ResponseEntity<byte[]> downloadDocumento(@PathVariable UUID id) {
    try {
        DocumentoComprobatorio doc = documentoService.buscarPorId(id);
        byte[] arquivoBytes = documentoService.buscarArquivoFisico(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getTipoConteudo())) // Define dinamicamente se é JPEG, PNG ou PDF
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getName() + "\"")
                .body(arquivoBytes);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
}

// Endpoint para Deleção (DELETE)
@DeleteMapping("/{id}")
public ResponseEntity<String> deletarDocumento(@PathVariable UUID id) {
    try {
        documentoService.deletarDocumento(id);
        return ResponseEntity.ok("Documento e arquivo físico removidos com sucesso.");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
}
