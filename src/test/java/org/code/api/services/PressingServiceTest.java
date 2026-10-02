package org.code.api.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.exception.PressingError;
import org.code.api.domain.models.pressing.Pressing;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.infrastructure.repositories.PressingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies organization-scoped reads; transactional posting is covered by ProcessingLedgerIT.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@ExtendWith(MockitoExtension.class)
class PressingServiceTest {
  @Mock PressingRepository pressingRepository;
  @Mock OrganizationScope scope;
  @InjectMocks PressingService service;

  /** Resolves records by organization independently of the creator identity. */
  @Test
  void readsOwnedRecord() {
    UUID org = UUID.randomUUID(), id = UUID.randomUUID();
    when(scope.organizationId()).thenReturn(org);
    when(pressingRepository.findByIdAndOrganizationId(id, org))
        .thenReturn(Optional.of(Pressing.builder().id(id).pressedBales(List.of()).build()));
    assertThat(service.getById(id).id()).isEqualTo(id);
  }

  /** Conceals absent and foreign records using the same domain failure. */
  @Test
  void hidesForeignRecord() {
    UUID org = UUID.randomUUID(), id = UUID.randomUUID();
    when(scope.organizationId()).thenReturn(org);
    when(pressingRepository.findByIdAndOrganizationId(id, org)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getById(id)).isInstanceOf(PressingError.NotFound.class);
  }
}
