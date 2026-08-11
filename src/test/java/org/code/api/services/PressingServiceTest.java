package org.code.api.services;

import org.code.api.domain.enums.DestinationType;
import org.code.api.domain.enums.OperationType;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.exception.PressingError;
import org.code.api.domain.models.inventory.InventoryBalance;
import org.code.api.domain.models.inventory.InventoryLog;
import org.code.api.domain.models.material.MaterialSubtype;
import org.code.api.domain.models.pressing.PressedBale;
import org.code.api.domain.models.pressing.Pressing;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.code.api.dto.pressing.request.PressedBaleRequestDTO;
import org.code.api.dto.pressing.request.PressingCreateRequestDTO;
import org.code.api.dto.pressing.response.PressingResponseDTO;
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
class PressingServiceTest {

    @Mock
    private PressingRepository pressingRepository;
    @Mock
    private PressedBaleRepository pressedBaleRepository;
    @Mock
    private SortedItemRepository sortedItemRepository;
    @Mock
    private MaterialSubtypeRepository subtypeRepository;
    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;
    @Mock
    private InventoryLogRepository inventoryLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticatedUserProvider userProvider;

    @InjectMocks
    private PressingService pressingService;

    private UUID userId;
    private User user;
    private UUID subtypeId;
    private MaterialSubtype subtype;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("user@test.com").build();
        subtypeId = UUID.randomUUID();
        subtype = MaterialSubtype.builder().id(subtypeId).name("Papelão").isActive(true).build();
    }

    @Test
    @DisplayName("Should create pressing, compact volume in inventory balance and save log")
    void create_ShouldSavePressing_AndUpdateVolumeInInventoryBalance_AndLogInventory() {
        when(userProvider.getCurrentUserId()).thenReturn(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Pressing savedPressing = Pressing.builder()
            .id(UUID.randomUUID())
            .pressingDate(OffsetDateTime.now())
            .creator(user)
            .isActive(true)
            .build();

        when(pressingRepository.save(any(Pressing.class))).thenReturn(savedPressing);
        when(subtypeRepository.findById(subtypeId)).thenReturn(Optional.of(subtype));

        PressedBale savedBale = PressedBale.builder()
            .id(UUID.randomUUID())
            .pressing(savedPressing)
            .materialSubtype(subtype)
            .weightKg(new BigDecimal("200.00"))
            .initialVolumeM3(new BigDecimal("5.00"))
            .finalVolumeM3(new BigDecimal("1.00"))
            .isActive(true)
            .build();

        when(pressedBaleRepository.save(any(PressedBale.class))).thenReturn(savedBale);

        InventoryBalance existingBalance = InventoryBalance.builder()
            .id(UUID.randomUUID())
            .materialSubtype(subtype)
            .currentWeightKg(new BigDecimal("500.00"))
            .currentVolumeM3(new BigDecimal("10.00"))
            .build();

        when(inventoryBalanceRepository.findByMaterialSubtypeId(subtypeId))
            .thenReturn(Optional.of(existingBalance));

        PressedBaleRequestDTO baleRequest = new PressedBaleRequestDTO(
            null,
            subtypeId,
            new BigDecimal("200.00"),
            new BigDecimal("5.00"),
            new BigDecimal("1.00"),
            DestinationType.STOCK,
            null
        );

        PressingCreateRequestDTO request = new PressingCreateRequestDTO(
            OffsetDateTime.now(),
            List.of(baleRequest)
        );

        PressingResponseDTO response = pressingService.create(request);

        assertNotNull(response);
        assertEquals(1, response.pressedBales().size());
        assertEquals(new BigDecimal("1.00"), response.pressedBales().get(0).finalVolumeM3());

        // Verify Inventory Balance volume compaction: 10.00 + (1.00 - 5.00) = 6.00 m3
        ArgumentCaptor<InventoryBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryBalance.class);
        verify(inventoryBalanceRepository).save(balanceCaptor.capture());
        assertEquals(new BigDecimal("6.00"), balanceCaptor.getValue().getCurrentVolumeM3());

        // Verify Inventory Log
        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        assertEquals(OperationType.PRESSING_OUTPUT, logCaptor.getValue().getOperationType());
        assertEquals(new BigDecimal("-4.00"), logCaptor.getValue().getQuantityM3());
    }

    @Test
    @DisplayName("Should throw MaterialError.NotFound when material subtype does not exist")
    void create_ShouldThrowException_WhenMaterialSubtypeNotFound() {
        when(userProvider.getCurrentUserId()).thenReturn(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Pressing savedPressing = Pressing.builder()
            .id(UUID.randomUUID())
            .pressingDate(OffsetDateTime.now())
            .creator(user)
            .isActive(true)
            .build();

        when(pressingRepository.save(any(Pressing.class))).thenReturn(savedPressing);
        when(subtypeRepository.findById(subtypeId)).thenReturn(Optional.empty());

        PressedBaleRequestDTO baleRequest = new PressedBaleRequestDTO(
            null,
            subtypeId,
            new BigDecimal("100.00"),
            new BigDecimal("3.00"),
            new BigDecimal("0.80"),
            DestinationType.STOCK,
            null
        );

        PressingCreateRequestDTO request = new PressingCreateRequestDTO(
            OffsetDateTime.now(),
            List.of(baleRequest)
        );

        assertThrows(MaterialError.NotFound.class, () -> pressingService.create(request));
        verify(inventoryBalanceRepository, never()).save(any());
        verify(inventoryLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get pressing record by ID when found and owned by user")
    void getById_ShouldReturnPressing_WhenRecordExists() {
        UUID pressingId = UUID.randomUUID();
        Pressing pressing = Pressing.builder()
            .id(pressingId)
            .pressingDate(OffsetDateTime.now())
            .creator(user)
            .isActive(true)
            .pressedBales(List.of())
            .build();

        when(userProvider.getCurrentUserId()).thenReturn(userId);
        when(pressingRepository.findByIdAndCreatorId(pressingId, userId)).thenReturn(Optional.of(pressing));

        PressingResponseDTO response = pressingService.getById(pressingId);

        assertNotNull(response);
        assertEquals(pressingId, response.id());
    }

    @Test
    @DisplayName("Should throw PressingError.NotFound when pressing record not found or not owned by user")
    void getById_ShouldThrowNotFound_WhenNotExists() {
        UUID pressingId = UUID.randomUUID();
        when(userProvider.getCurrentUserId()).thenReturn(userId);
        when(pressingRepository.findByIdAndCreatorId(pressingId, userId)).thenReturn(Optional.empty());

        assertThrows(PressingError.NotFound.class, () -> pressingService.getById(pressingId));
    }
}
