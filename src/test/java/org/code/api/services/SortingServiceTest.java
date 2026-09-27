package org.code.api.services;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.ports.OrganizationScope;
import org.code.api.domain.models.sorting.Sorting;
import org.code.api.domain.exception.SortingError;
import org.code.api.infrastructure.repositories.SortingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Verifies organization-scoped reads; transactional posting is covered by ProcessingLedgerIT.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@ExtendWith(MockitoExtension.class)
class SortingServiceTest {
    @Mock SortingRepository sortingRepository;
    @Mock OrganizationScope scope;
    @InjectMocks SortingService service;
    /** Resolves records by organization independently of the creator identity. */
    @Test void readsOwnedRecord() {
        UUID org=UUID.randomUUID(), id=UUID.randomUUID();
        when(scope.organizationId()).thenReturn(org);
        when(sortingRepository.findByIdAndOrganizationId(id,org)).thenReturn(Optional.of(Sorting.builder().id(id).sortedItems(List.of()).build()));
        assertThat(service.getById(id).id()).isEqualTo(id);
    }
    /** Conceals absent and foreign records using the same domain failure. */
    @Test void hidesForeignRecord() {
        UUID org=UUID.randomUUID(), id=UUID.randomUUID();
        when(scope.organizationId()).thenReturn(org);
        when(sortingRepository.findByIdAndOrganizationId(id,org)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.getById(id)).isInstanceOf(SortingError.NotFound.class);
    }
}
