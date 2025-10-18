package org.code.api.domain.models.document;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "documento",
        indexes = {
                @Index(name = "idx_documento_nome", columnList = "nome")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", length = 191)
    private String nome;

    @Column(name = "data_documento")
    private LocalDateTime dataDocumento;

    @Column(name = "data_insercao")
    private LocalDateTime dataInsercao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private DocumentType tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "documento_media_id_fk",
                    foreignKeyDefinition = "FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE SET NULL ON UPDATE CASCADE"
            )
    )
    private Media mediaId;
}
