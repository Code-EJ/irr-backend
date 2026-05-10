package org.code.api.domain.models.sorting;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.enums.SortingType;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela {@code sorting}.
 * Representa uma operação de triagem de materiais (GROSS, PRIMARY, FINE).
 */
@Entity
@Table(name = "sorting")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Sorting extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sorting_date")
    private OffsetDateTime sortingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sorting_type", length = 50)
    private SortingType sortingType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "sorting")
    @Builder.Default
    private List<SortedItem> sortedItems = new ArrayList<>();
}
