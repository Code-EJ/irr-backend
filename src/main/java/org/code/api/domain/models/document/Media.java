package org.code.api.domain.models.document;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "media"
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "dados", nullable = false)
    private byte[] dados;
}
