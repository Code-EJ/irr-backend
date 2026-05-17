package org.code.api.services;

import org.code.api.domain.models.documents.DocumentoComprobatorio;
import org.code.api.infrastructure.repositories.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class DocumentoService {

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private StorageService storageService; // Injeta a abstração local automaticamente

    @Transactional
    public DocumentoComprobatorio registrarDocumento(MultipartFile arquivo) throws IOException {
        // 1. Envia para o servidor e extrai a String do caminho
        String caminhoServidor = storageService.armazenar(arquivo);

        // 2. Salva as informações textuais no PostgreSQL
        DocumentoComprobatorio doc = new DocumentoComprobatorio();
        doc.setName(arquivo.getOriginalFilename());
        doc.setTipoConteudo(arquivo.getContentType());
        doc.setCaminhoArmazenamento(caminhoServidor);

        return repository.save(doc);
    }

    @Transactional(readOnly = true)
public byte[] buscarArquivoFisico(UUID id) throws IOException {
    DocumentoComprobatorio doc = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Documento não encontrado com o ID fornecido"));

    // Lê os bytes diretamente do arquivo armazenado no caminho salvo
    return Files.readAllBytes(Paths.get(doc.getCaminhoArmazenamento()));
}
public DocumentoComprobatorio buscarPorId (UUID id) {
    DocumentoComprobatorio doc = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Documento não encontrado com o ID fornecido"));
            return doc;
}

// Método para deletar o registro do banco e o arquivo físico do servidor mock
@Transactional
public void deletarDocumento(UUID id) {
    DocumentoComprobatorio doc = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Documento não encontrado com o ID fornecido"));

    try {
        // 1. Apaga o arquivo físico da pasta simulada
        Files.deleteIfExists(Paths.get(doc.getCaminhoArmazenamento()));
    } catch (IOException e) {
        throw new RuntimeException("Falha ao apagar o arquivo físico do servidor mock", e);
    }

    // 2. Apaga o registro textual correspondente no PostgreSQL
    repository.delete(doc);
}
}