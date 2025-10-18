package org.code.api.domain.models;


import jakarta.persistence.*;
import lombok.*;
import org.code.api.utils.TimeStampedEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "usuario",
        indexes = {
                @Index(name = "idx_usuario_email", columnList = "email", unique = true)
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class User extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 191)
    private String email;

    @Column(name = "senha", nullable = false, length = 191)
    private String senha;

    @Column(name = "nome", nullable = false, length = 191)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoUsuario tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "criador_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(
                    name = "usuario_criador_id_fkey",
                    foreignKeyDefinition = "FOREIGN KEY (criador_id) REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE"
            )
    )
    private User criador_id;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criado_em;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizado_em;

    public enum TipoUsuario {
        ADMINISTRADOR,
        PREFEITURA,
        ORGANIZACAO,
        REPRESENTANTE
    }
}