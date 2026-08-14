package org.code.api.utils;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.code.api.domain.user.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class TimeStampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criadorId", referencedColumnName = "id", nullable = true)
    private User createdBy;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "atualizado_em")
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
