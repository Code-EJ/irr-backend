package org.code.api.domain.models.collection;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.models.base.Attachment;
import org.code.api.domain.models.base.TeamMember;
import org.code.api.domain.models.base.Vehicle;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Organization-owned raw intake with fleet, team, evidence and optional route logistics.
  * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Entity
@Table(name = "collection")
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Collection extends TimeStampedEntity {
    @Column(name="route_description",length=2000) private String routeDescription;
    @Column(name="departure_at") private OffsetDateTime departureAt;
    @Column(name="arrival_at") private OffsetDateTime arrivalAt;
    @Column(name="distance_km",precision=12,scale=3) private BigDecimal distanceKm;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "realization_date", nullable = false)
    private OffsetDateTime realizationDate;

    @Column(name = "total_weight_kg", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalWeightKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private TeamMember driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mtr_generator_id")
    private Attachment mtrGenerator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mtr_destinator_id")
    private Attachment mtrDestinator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_diary_id")
    private Attachment collectionDiary;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToMany
    @JoinTable(
            name = "collection_team",
            joinColumns = @JoinColumn(name = "collection_id"),
            inverseJoinColumns = @JoinColumn(name = "team_member_id")
    )
    @Builder.Default
    private Set<TeamMember> teamMembers = new HashSet<>();

    @OneToMany(mappedBy = "collection")
    @Builder.Default
    private List<InputItem> inputItems = new ArrayList<>();
}
