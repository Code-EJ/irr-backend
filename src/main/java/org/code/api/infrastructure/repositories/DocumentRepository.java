package org.code.api.infrastructure.repositories;

import java.util.UUID;
import org.code.api.domain.models.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {}
