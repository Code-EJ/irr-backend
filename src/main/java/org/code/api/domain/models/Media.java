package org.code.api.domain.models;

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
    private Integer id;

    @Column(name = "dados", nullable = false)
    private byte[] dados;
}
