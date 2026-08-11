package org.code.api.services;

import org.code.api.domain.enums.OperationType;
import org.code.api.domain.enums.SortingType;
import org.code.api.domain.enums.DestinationType;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.exception.SortingError;
import org.code.api.domain.models.inventory.InventoryBalance;
import org.code.api.domain.models.inventory.InventoryLog;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.sorting.SortedItem;
import org.code.api.domain.models.sorting.Sorting;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.dto.sorting.request.SortedItemRequestDTO;
import org.code.api.dto.sorting.request.SortingCreateRequestDTO;
import org.code.api.dto.sorting.response.SortingResponseDTO;
import org.code.api.infrastructure.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SortingServiceTest {

        @Mock
        private SortingRepository sortingRepository;
        @Mock
        private SortedItemRepository sortedItemRepository;
        @Mock
        private MaterialSubtypeRepository subtypeRepository;
        @Mock
        private InputItemRepository inputItemRepository;
        @Mock
        private InventoryBalanceRepository inventoryBalanceRepository;
        @Mock
        private InventoryLogRepository inventoryLogRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private AuthenticatedUserProvider userProvider;

        @InjectMocks
        private SortingService sortingService;

        private UUID userId;
        private User user;
        private UUID subtypeId;
        private MaterialSubtype subtype;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                user = User.builder().id(userId).email("user@test.com").build();
                subtypeId = UUID.randomUUID();
                subtype = MaterialSubtype.builder().id(subtypeId).name("PET Clára").isActive(true).build();
        }

        @Test
        @DisplayName("Should create sorting, update inventory balance and save inventory log with reject control")
        void create_ShouldSaveSorting_AndUpdateInventoryBalance_AndLogInventory() {
                when(userProvider.getCurrentUserId()).thenReturn(userId);
                when(userRepository.getReferenceById(userId)).thenReturn(user);

                Sorting savedSorting = Sorting.builder()
                                .id(UUID.randomUUID())
                                .sortingDate(OffsetDateTime.now())
                                .sortingType(SortingType.GROSS)
                                .creator(user)
                                .isActive(true)
                                .build();

                when(sortingRepository.save(any(Sorting.class))).thenReturn(savedSorting);
                when(subtypeRepository.findById(subtypeId)).thenReturn(Optional.of(subtype));

                SortedItem savedSortedItem = SortedItem.builder()
                                .id(UUID.randomUUID())
                                .sorting(savedSorting)
                                .materialSubtype(subtype)
                                .weightKg(new BigDecimal("150.00"))
                                .volumeM3(new BigDecimal("2.50"))
                                .rejectWeightKg(new BigDecimal("10.00"))
                                .rejectVolumeM3(new BigDecimal("0.20"))
                                .isActive(true)
                                .build();

                when(sortedItemRepository.save(any(SortedItem.class))).thenReturn(savedSortedItem);

                InventoryBalance existingBalance = InventoryBalance.builder()
                                .id(UUID.randomUUID())
                                .materialSubtype(subtype)
                                .currentWeightKg(new BigDecimal("50.00"))
                                .currentVolumeM3(new BigDecimal("1.00"))
                                .build();

                when(inventoryBalanceRepository.findByMaterialSubtypeId(subtypeId))
                                .thenReturn(Optional.of(existingBalance));

                SortedItemRequestDTO itemRequest = new SortedItemRequestDTO(
                                null,
                                subtypeId,
                                new BigDecimal("150.00"),
                                new BigDecimal("2.50"),
                                new BigDecimal("10.00"),
                                new BigDecimal("0.20"),
                                DestinationType.STOCK,
                                null);

                SortingCreateRequestDTO request = new SortingCreateRequestDTO(
                                OffsetDateTime.now(),
                                SortingType.GROSS,
                                List.of(itemRequest));

                SortingResponseDTO response = sortingService.create(request);

                assertNotNull(response);
                assertEquals(SortingType.GROSS, response.sortingType());
                assertEquals(1, response.sortedItems().size());
                assertEquals(new BigDecimal("150.00"), response.sortedItems().get(0).weightKg());
                assertEquals(new BigDecimal("10.00"), response.sortedItems().get(0).rejectWeightKg());

                // Verify Inventory Balance update uses NET weight/volume (gross - reject)
                ArgumentCaptor<InventoryBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryBalance.class);
                verify(inventoryBalanceRepository).save(balanceCaptor.capture());
                assertEquals(new BigDecimal("190.00"), balanceCaptor.getValue().getCurrentWeightKg());
                assertEquals(new BigDecimal("3.30"), balanceCaptor.getValue().getCurrentVolumeM3());

                // Verify two Inventory Log entries: SORTING_OUTPUT (net) + MANUAL_ADJUSTMENT (reject)
                ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
                verify(inventoryLogRepository, times(2)).save(logCaptor.capture());
                List<InventoryLog> savedLogs = logCaptor.getAllValues();

                InventoryLog sortingOutputLog = savedLogs.stream()
                                .filter(l -> l.getOperationType() == OperationType.SORTING_OUTPUT)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("SORTING_OUTPUT log not found"));
                assertEquals(new BigDecimal("140.00"), sortingOutputLog.getQuantityKg());
                assertEquals(new BigDecimal("2.30"), sortingOutputLog.getQuantityM3());

                InventoryLog rejectLog = savedLogs.stream()
                                .filter(l -> l.getOperationType() == OperationType.MANUAL_ADJUSTMENT)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("MANUAL_ADJUSTMENT reject log not found"));
                assertEquals(new BigDecimal("-10.00"), rejectLog.getQuantityKg());
                assertEquals(new BigDecimal("-0.20"), rejectLog.getQuantityM3());
        }

        @Test
        @DisplayName("Should throw MaterialError.NotFound when material subtype does not exist or is inactive")
        void create_ShouldThrowException_WhenMaterialSubtypeNotFound() {
                when(userProvider.getCurrentUserId()).thenReturn(userId);
                when(userRepository.getReferenceById(userId)).thenReturn(user);

                Sorting savedSorting = Sorting.builder()
                                .id(UUID.randomUUID())
                                .sortingDate(OffsetDateTime.now())
                                .sortingType(SortingType.PRIMARY)
                                .creator(user)
                                .isActive(true)
                                .build();

                when(sortingRepository.save(any(Sorting.class))).thenReturn(savedSorting);
                when(subtypeRepository.findById(subtypeId)).thenReturn(Optional.empty());

                SortedItemRequestDTO itemRequest = new SortedItemRequestDTO(
                                null,
                                subtypeId,
                                new BigDecimal("100.00"),
                                new BigDecimal("2.00"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                DestinationType.STOCK,
                                null);

                SortingCreateRequestDTO request = new SortingCreateRequestDTO(
                                OffsetDateTime.now(),
                                SortingType.PRIMARY,
                                List.of(itemRequest));

                assertThrows(MaterialError.NotFound.class, () -> sortingService.create(request));
                verify(inventoryBalanceRepository, never()).save(any());
                verify(inventoryLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should compute net inventory balance (gross - reject) and log reject disposal")
        void should_compute_net_inventory_balance_after_subtracting_reject() {
                when(userProvider.getCurrentUserId()).thenReturn(userId);
                when(userRepository.getReferenceById(userId)).thenReturn(user);

                Sorting savedSorting = Sorting.builder()
                                .id(UUID.randomUUID())
                                .sortingDate(OffsetDateTime.now())
                                .sortingType(SortingType.GROSS)
                                .creator(user)
                                .isActive(true)
                                .build();

                when(sortingRepository.save(any(Sorting.class))).thenReturn(savedSorting);
                when(subtypeRepository.findById(subtypeId)).thenReturn(Optional.of(subtype));

                SortedItem savedSortedItem = SortedItem.builder()
                                .id(UUID.randomUUID())
                                .sorting(savedSorting)
                                .materialSubtype(subtype)
                                .weightKg(new BigDecimal("100.00"))
                                .volumeM3(new BigDecimal("10.00"))
                                .rejectWeightKg(new BigDecimal("20.00"))
                                .rejectVolumeM3(new BigDecimal("2.00"))
                                .isActive(true)
                                .build();

                when(sortedItemRepository.save(any(SortedItem.class))).thenReturn(savedSortedItem);

                when(inventoryBalanceRepository.findByMaterialSubtypeId(subtypeId))
                                .thenReturn(Optional.empty());

                SortedItemRequestDTO itemRequest = new SortedItemRequestDTO(
                                null,
                                subtypeId,
                                new BigDecimal("100.00"),
                                new BigDecimal("10.00"),
                                new BigDecimal("20.00"),
                                new BigDecimal("2.00"),
                                DestinationType.STOCK,
                                null);

                SortingCreateRequestDTO request = new SortingCreateRequestDTO(
                                OffsetDateTime.now(),
                                SortingType.GROSS,
                                List.of(itemRequest));

                SortingResponseDTO response = sortingService.create(request);

                assertNotNull(response);

                ArgumentCaptor<InventoryBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryBalance.class);
                verify(inventoryBalanceRepository).save(balanceCaptor.capture());
                assertEquals(new BigDecimal("80.00"), balanceCaptor.getValue().getCurrentWeightKg());
                assertEquals(new BigDecimal("8.00"), balanceCaptor.getValue().getCurrentVolumeM3());

                ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
                verify(inventoryLogRepository, times(2)).save(logCaptor.capture());
                List<InventoryLog> savedLogs = logCaptor.getAllValues();

                InventoryLog sortingOutputLog = savedLogs.stream()
                                .filter(l -> l.getOperationType() == OperationType.SORTING_OUTPUT)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("SORTING_OUTPUT log not found"));
                assertEquals(new BigDecimal("80.00"), sortingOutputLog.getQuantityKg());
                assertEquals(new BigDecimal("8.00"), sortingOutputLog.getQuantityM3());

                InventoryLog rejectLog = savedLogs.stream()
                                .filter(l -> l.getOperationType() == OperationType.MANUAL_ADJUSTMENT)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("MANUAL_ADJUSTMENT reject log not found"));
                assertEquals(new BigDecimal("-20.00"), rejectLog.getQuantityKg());
                assertEquals(new BigDecimal("-2.00"), rejectLog.getQuantityM3());
        }

        @Test
        @DisplayName("Should get sorting record by ID when found and owned by user")
        void getById_ShouldReturnSorting_WhenRecordExists() {
                UUID sortingId = UUID.randomUUID();
                Sorting sorting = Sorting.builder()
                                .id(sortingId)
                                .sortingDate(OffsetDateTime.now())
                                .sortingType(SortingType.FINE)
                                .creator(user)
                                .isActive(true)
                                .sortedItems(List.of())
                                .build();

                when(userProvider.getCurrentUserId()).thenReturn(userId);
                when(sortingRepository.findByIdAndCreatorId(sortingId, userId)).thenReturn(Optional.of(sorting));

                SortingResponseDTO response = sortingService.getById(sortingId);

                assertNotNull(response);
                assertEquals(sortingId, response.id());
                assertEquals(SortingType.FINE, response.sortingType());
        }

        @Test
        @DisplayName("Should throw SortingError.NotFound when sorting is not found or not owned by user")
        void getById_ShouldThrowNotFound_WhenNotExists() {
                UUID sortingId = UUID.randomUUID();
                when(userProvider.getCurrentUserId()).thenReturn(userId);
                when(sortingRepository.findByIdAndCreatorId(sortingId, userId)).thenReturn(Optional.empty());

                assertThrows(SortingError.NotFound.class, () -> sortingService.getById(sortingId));
        }
}
