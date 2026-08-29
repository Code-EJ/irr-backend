package org.code.api.domain.models.base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela {@code attachment}.
 * Centraliza referências a documentos anexos (NFe, MTR, Recibos, etc.)
 * usados pelas tabelas operacionais.
 */
@Entity
@Table(name = "attachment")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Attachment extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 900)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "storage_url", nullable = false)
    private String storageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
