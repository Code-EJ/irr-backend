package org.code.api.services;

import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.infrastructure.repositories.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private AttachmentRepository repository;

    @Autowired
    private StorageService storageService; // Injeta a abstração local automaticamente



    @Transactional
    public Attachment registerDocument(MultipartFile arquivo, UUID creatorID) throws IOException {
        // 1. Envia para o servidor e extrai a String do caminho
        String path = storageService.armazenar(arquivo);

        // 2. Salva as informações textuais no PostgreSQL
        Attachment doc = new Attachment();
        doc.setFileName(arquivo.getOriginalFilename());
        doc.setFileType(arquivo.getContentType());
        doc.setStorageUrl(path);
        doc.setIsActive(true);

        // Associa o creator (se for relacionamento com User)
        User creator = User.builder().id(creatorID).build();
        doc.setCreator(creator);


        return repository.save(doc);
    }

    @Transactional(readOnly = true)
    public byte[] findLocalArchives(UUID id) throws IOException {
        Attachment doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado com o ID fornecido"));

        // Lê os bytes diretamente do arquivo armazenado no caminho salvo
        return Files.readAllBytes(Paths.get(doc.getStorageUrl()));
    }

    public Attachment findById(UUID id) {
        Attachment doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado com o ID fornecido"));
        return doc;
    }

    // Método para deletar o registro do banco e o arquivo físico do servidor mock
    @Transactional
    public void deleteDocument(UUID id) {
        Attachment doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado com o ID fornecido"));

        try {
            // 1. Apaga o arquivo físico da pasta simulada
            Files.deleteIfExists(Paths.get(doc.getStorageUrl()));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao apagar o arquivo físico do servidor mock", e);
        }

        // 2. Apaga o registro textual correspondente no PostgreSQL
        repository.delete(doc);
    }
}