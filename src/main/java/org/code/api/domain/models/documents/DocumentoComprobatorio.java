package org.code.api.domain.models.documents;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "documento_comprobatorio")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoComprobatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tipoConteudo; // Guarda o MIME Type (ex: application/pdf, image/jpeg)

    @Column(name = "caminho_armazenamento", nullable = false, length = 512)
    private String caminhoArmazenamento; // O caminho físico do arquivo no servidor
}