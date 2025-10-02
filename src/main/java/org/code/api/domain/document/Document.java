package org.code.api.domain.document;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.code.api.domain.media.Media;
import org.code.api.utils.TimeStampedEntity;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;
import java.util.UUID;

@Entity
@Table( name = "documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document extends TimeStampedEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "dataDocumento")
    private Date dataDocumento;

    @Column(name = "dataInsercao")
    private Date dataInsercao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mediaId", referencedColumnName = "id")
    private Media mediaId;
}
