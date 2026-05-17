package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;

import org.code.api.domain.models.documents.DocumentoComprobatorio;
import org.springframework.data.jpa.repository.JpaRepository;


public interface DocumentRepository extends JpaRepository<DocumentoComprobatorio,UUID> {

    Optional<DocumentoComprobatorio> findByName(String fileName);

}
