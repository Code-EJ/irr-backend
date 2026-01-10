package org.code.api.domain.models.document;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.code.api.domain.enums.DocumentType;
import org.code.api.domain.models.user.User;

@Entity
@Table(
    name = "documento",
    indexes = { @Index(name = "idx_documento_nome", columnList = "nome") }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", length = 191)
    private String nome;

    @Column(name = "data_documento")
    private LocalDateTime dataDocumento;

    @Column(name = "data_insercao", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "data_remocao", nullable = false, updatable = false)
    private LocalDateTime deletedAt;

    @Column(name = "deletado", nullable = false)
    private boolean deletado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private DocumentType tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "media_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(
            name = "documento_media_id_fk",
            foreignKeyDefinition = "FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE SET NULL ON UPDATE CASCADE"
        )
    )
    private Media mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "criador_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(
            name = "criador_id_fk",
            foreignKeyDefinition = "FOREIGN KEY (criador_id) REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE"
        ),
        nullable = false
    )
    private User createdBy;
}
