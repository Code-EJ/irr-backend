package org.code.api.domain.models.user;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

import org.code.api.domain.common.TimeStampedEntity;
import org.code.api.domain.enums.UserType;

@Entity
@Table(
  name = "usuario",
  indexes = {
    @Index(name = "idx_usuario_email", columnList = "email", unique = true),
  }
)
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
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
  private UserType tipo;
}
