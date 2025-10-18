package org.code.api.utils;


import jakarta.persistence.*;
import org.code.api.domain.models.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class TimeStampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criador_id", referencedColumnName = "id", nullable = false)
    private User createdBy;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "atualizado_em", nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
