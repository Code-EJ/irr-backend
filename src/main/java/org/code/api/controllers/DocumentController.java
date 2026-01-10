package org.code.api.controllers;

import java.util.List;
import org.code.api.dto.document.DocumentResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documento")
public class DocumentController {

    // @Autowired
    // private DocumentService documentService;
    // @Autowired
    // private DocumentRepository documentRepository;
    @GetMapping("list")
    public ResponseEntity<List<DocumentResponseDTO>> listDocuments() {
        return ResponseEntity.ok().build();
    }
    // @GetMapping("get/{id}")
    // public ResponseEntity<DocumentResponseDTO> getDocument(
    //   @PathVariable UUID id,
    //   @RequestHeader(value = "X-Api-Key") String token
    // ) {
    //   Document document = documentRepository.getById(id);
    //   DocumentResponseDTO responseDTO = new DocumentResponseDTO(document);
    //   return ResponseEntity.ok(responseDTO);
    // }
}
