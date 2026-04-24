package org.code.api.domain.common;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.code.api.domain.models.user.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class TimeStampedEntity {

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

  @Column(name = "data_criacao", nullable = true, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "data_atualizacao", nullable = true, updatable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
