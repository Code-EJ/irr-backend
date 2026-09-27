# Current HTTP API and DTO inventory

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Official HTTP documentation is served by Swagger UI at /swagger-ui/index.html and OpenAPI at /v3/api-docs. This file is supporting source-audit material.

Generated from controller/record declarations on 2026-09-26. This documents implemented declarations, not proof of correct authorization or a replacement for request/response integration tests. Base URL in the development Compose stack: http://localhost:9191.

## Conventions and compatibility

Bearer JWT is required outside exact login, health and enabled documentation routes. Session authenticate accepts email/password and returns the token DTO. POST /api/users is administrator-only and returns safe partner metadata; POST /api/session/register is deprecated and returns 410 after authentication. Attachments use creator-scoped safe metadata and durable deletion; see ADR-0008 and the running Swagger specification. Collection endpoints generally return Spring Page (page/size/sort); UUID path IDs and PUT updates differ from the frontend's old numeric/Portuguese/PATCH contracts. Field validation is declared in the linked records. Identity and attachment boundary failures use safe English responses; unrelated legacy domain error shapes still require consolidation.

The document upload still accepts the legacy multipart field documento; its HTTP rename to file requires an explicit frontend contract change. Entity response and object-authorization defects remain next-stage work. The configurable storage adapter change does not fix them. No collection, team or sale controller exists yet.

## Operations

| Method | Path | Declared method authorization | Source |
| --- | --- | --- | --- |
| POST | `/api/documents` | No method annotation; see filter and service behavior | [src/main/java/org/code/api/controllers/DocumentController.java](../src/main/java/org/code/api/controllers/DocumentController.java) |
| GET | `/api/documents/{id}/download` | No method annotation; see filter and service behavior | [src/main/java/org/code/api/controllers/DocumentController.java](../src/main/java/org/code/api/controllers/DocumentController.java) |
| DELETE | `/api/documents/{id}` | No method annotation; see filter and service behavior | [src/main/java/org/code/api/controllers/DocumentController.java](../src/main/java/org/code/api/controllers/DocumentController.java) |
| POST | `/api/donations` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/DonationController.java](../src/main/java/org/code/api/controllers/DonationController.java) |
| GET | `/api/donations` | isAuthenticated() | [src/main/java/org/code/api/controllers/DonationController.java](../src/main/java/org/code/api/controllers/DonationController.java) |
| GET | `/api/donations/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/DonationController.java](../src/main/java/org/code/api/controllers/DonationController.java) |
| PUT | `/api/donations/{id}` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/DonationController.java](../src/main/java/org/code/api/controllers/DonationController.java) |
| DELETE | `/api/donations/{id}` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/DonationController.java](../src/main/java/org/code/api/controllers/DonationController.java) |
| POST | `/api/donors` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/DonorController.java](../src/main/java/org/code/api/controllers/DonorController.java) |
| GET | `/api/donors` | isAuthenticated() | [src/main/java/org/code/api/controllers/DonorController.java](../src/main/java/org/code/api/controllers/DonorController.java) |
| GET | `/api/donors/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/DonorController.java](../src/main/java/org/code/api/controllers/DonorController.java) |
| PUT | `/api/donors/{id}` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/DonorController.java](../src/main/java/org/code/api/controllers/DonorController.java) |
| DELETE | `/api/donors/{id}` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/DonorController.java](../src/main/java/org/code/api/controllers/DonorController.java) |
| POST | `/api/materials/categories` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialCategoryController.java](../src/main/java/org/code/api/controllers/MaterialCategoryController.java) |
| GET | `/api/materials/categories` | isAuthenticated() | [src/main/java/org/code/api/controllers/MaterialCategoryController.java](../src/main/java/org/code/api/controllers/MaterialCategoryController.java) |
| GET | `/api/materials/categories/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/MaterialCategoryController.java](../src/main/java/org/code/api/controllers/MaterialCategoryController.java) |
| PUT | `/api/materials/categories/{id}` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialCategoryController.java](../src/main/java/org/code/api/controllers/MaterialCategoryController.java) |
| DELETE | `/api/materials/categories/{id}` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialCategoryController.java](../src/main/java/org/code/api/controllers/MaterialCategoryController.java) |
| POST | `/api/materials/subtypes` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialSubtypeController.java](../src/main/java/org/code/api/controllers/MaterialSubtypeController.java) |
| GET | `/api/materials/subtypes` | isAuthenticated() | [src/main/java/org/code/api/controllers/MaterialSubtypeController.java](../src/main/java/org/code/api/controllers/MaterialSubtypeController.java) |
| GET | `/api/materials/subtypes/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/MaterialSubtypeController.java](../src/main/java/org/code/api/controllers/MaterialSubtypeController.java) |
| PUT | `/api/materials/subtypes/{id}` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialSubtypeController.java](../src/main/java/org/code/api/controllers/MaterialSubtypeController.java) |
| DELETE | `/api/materials/subtypes/{id}` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialSubtypeController.java](../src/main/java/org/code/api/controllers/MaterialSubtypeController.java) |
| POST | `/api/materials/types` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialTypeController.java](../src/main/java/org/code/api/controllers/MaterialTypeController.java) |
| GET | `/api/materials/types` | isAuthenticated() | [src/main/java/org/code/api/controllers/MaterialTypeController.java](../src/main/java/org/code/api/controllers/MaterialTypeController.java) |
| GET | `/api/materials/types/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/MaterialTypeController.java](../src/main/java/org/code/api/controllers/MaterialTypeController.java) |
| PUT | `/api/materials/types/{id}` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialTypeController.java](../src/main/java/org/code/api/controllers/MaterialTypeController.java) |
| DELETE | `/api/materials/types/{id}` | hasRole('ADMINISTRATOR') | [src/main/java/org/code/api/controllers/MaterialTypeController.java](../src/main/java/org/code/api/controllers/MaterialTypeController.java) |
| POST | `/api/pressings` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/PressingController.java](../src/main/java/org/code/api/controllers/PressingController.java) |
| GET | `/api/pressings` | isAuthenticated() | [src/main/java/org/code/api/controllers/PressingController.java](../src/main/java/org/code/api/controllers/PressingController.java) |
| GET | `/api/pressings/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/PressingController.java](../src/main/java/org/code/api/controllers/PressingController.java) |
| POST | `/api/session/register` | Authenticated retirement response (410); no provisioning | [src/main/java/org/code/api/controllers/SessionController.java](../src/main/java/org/code/api/controllers/SessionController.java) |
| POST | `/api/session/authenticate` | No method annotation; see filter and service behavior | [src/main/java/org/code/api/controllers/SessionController.java](../src/main/java/org/code/api/controllers/SessionController.java) |
| POST | `/api/sortings` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/SortingController.java](../src/main/java/org/code/api/controllers/SortingController.java) |
| GET | `/api/sortings` | isAuthenticated() | [src/main/java/org/code/api/controllers/SortingController.java](../src/main/java/org/code/api/controllers/SortingController.java) |
| GET | `/api/sortings/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/SortingController.java](../src/main/java/org/code/api/controllers/SortingController.java) |
| POST | `/api/vehicles` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/VehicleController.java](../src/main/java/org/code/api/controllers/VehicleController.java) |
| POST | `/api/vehicles/batch` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/VehicleController.java](../src/main/java/org/code/api/controllers/VehicleController.java) |
| GET | `/api/vehicles` | isAuthenticated() | [src/main/java/org/code/api/controllers/VehicleController.java](../src/main/java/org/code/api/controllers/VehicleController.java) |
| GET | `/api/vehicles/{id}` | isAuthenticated() | [src/main/java/org/code/api/controllers/VehicleController.java](../src/main/java/org/code/api/controllers/VehicleController.java) |
| PUT | `/api/vehicles/{id}` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/VehicleController.java](../src/main/java/org/code/api/controllers/VehicleController.java) |
| PUT | `/api/vehicles/batch` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/VehicleController.java](../src/main/java/org/code/api/controllers/VehicleController.java) |
| DELETE | `/api/vehicles/{id}` | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | [src/main/java/org/code/api/controllers/VehicleController.java](../src/main/java/org/code/api/controllers/VehicleController.java) |

## DTO field declarations

Types and Jakarta validation below are taken directly from Java records. They are current wire inputs/results; nested records are linked by their source names. No schema is inferred from issue titles.

### AttachmentCreateRequestDTO

[src/main/java/org/code/api/dto/attachment/request/AttachmentCreateRequestDTO.java](../src/main/java/org/code/api/dto/attachment/request/AttachmentCreateRequestDTO.java)

~~~java
public record AttachmentCreateRequestDTO(
    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must be at most 255 characters")
    String fileName,
    @NotBlank(message = "File type is required")
    @Size(max = 50, message = "File type must be at most 50 characters")
    String fileType,
    @NotBlank(message = "Storage URL is required")
    String storageUrl
) {}
~~~

### AttachmentResponseDTO

[src/main/java/org/code/api/dto/attachment/response/AttachmentResponseDTO.java](../src/main/java/org/code/api/dto/attachment/response/AttachmentResponseDTO.java)

~~~java
public record AttachmentResponseDTO(
    UUID id,
    String fileName,
    String fileType,
    String storageUrl,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### CollectionCreateRequestDTO

[src/main/java/org/code/api/dto/collection/request/CollectionCreateRequestDTO.java](../src/main/java/org/code/api/dto/collection/request/CollectionCreateRequestDTO.java)

~~~java
public record CollectionCreateRequestDTO(
    @NotNull(message = "Realization date is required")
    OffsetDateTime realizationDate,
    @NotNull(message = "Total weight is required")
    @Positive(message = "Total weight must be positive")
    BigDecimal totalWeightKg,
    @NotNull(message = "Vehicle ID is required")
    UUID vehicleId,
    @NotNull(message = "Driver ID is required")
    UUID driverId,
    UUID mtrGeneratorId,
    UUID mtrDestinatorId,
    UUID collectionDiaryId,
    Set<UUID> teamMemberIds,
    @Valid
    List<InputItemRequestDTO> inputItems
) {}
~~~

### InputItemRequestDTO

[src/main/java/org/code/api/dto/collection/request/InputItemRequestDTO.java](../src/main/java/org/code/api/dto/collection/request/InputItemRequestDTO.java)

~~~java
public record InputItemRequestDTO(
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    BigDecimal weightKg,
    @NotNull(message = "Volume is required")
    @Positive(message = "Volume must be positive")
    BigDecimal volumeM3
) {}
~~~

### CollectionResponseDTO

[src/main/java/org/code/api/dto/collection/response/CollectionResponseDTO.java](../src/main/java/org/code/api/dto/collection/response/CollectionResponseDTO.java)

~~~java
public record CollectionResponseDTO(
    UUID id,
    OffsetDateTime realizationDate,
    BigDecimal totalWeightKg,
    UUID vehicleId,
    UUID driverId,
    UUID mtrGeneratorId,
    UUID mtrDestinatorId,
    UUID collectionDiaryId,
    Boolean isActive,
    Set<UUID> teamMemberIds,
    List<InputItemResponseDTO> inputItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### InputItemResponseDTO

[src/main/java/org/code/api/dto/collection/response/InputItemResponseDTO.java](../src/main/java/org/code/api/dto/collection/response/InputItemResponseDTO.java)

~~~java
public record InputItemResponseDTO(
    UUID id,
    UUID collectionId,
    UUID donationId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal volumeM3,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### DonationCreateRequestDTO

[src/main/java/org/code/api/dto/donation/request/DonationCreateRequestDTO.java](../src/main/java/org/code/api/dto/donation/request/DonationCreateRequestDTO.java)

~~~java
public record DonationCreateRequestDTO(
    OffsetDateTime donationDate,
    @NotNull(message = "Total weight is required")
    @Positive(message = "Total weight must be positive")
    BigDecimal totalWeightKg,
    @NotNull(message = "Donor ID is required")
    UUID donorId,
    UUID proofAttachmentId,
    @Valid
    List<InputItemRequestDTO> inputItems
) {}
~~~

### DonationUpdateRequestDTO

[src/main/java/org/code/api/dto/donation/request/DonationUpdateRequestDTO.java](../src/main/java/org/code/api/dto/donation/request/DonationUpdateRequestDTO.java)

~~~java
public record DonationUpdateRequestDTO(
        OffsetDateTime donationDate,
        @NotNull(message = "Total weight is required")
        @Positive(message = "Total weight must be positive")
        BigDecimal totalWeightKg,
        UUID proofAttachmentId,
        @NotEmpty(message = "At least one input item is required")
        @Valid
        List<InputItemRequestDTO> inputItems
) {}
~~~

### DonationResponseDTO

[src/main/java/org/code/api/dto/donation/response/DonationResponseDTO.java](../src/main/java/org/code/api/dto/donation/response/DonationResponseDTO.java)

~~~java
public record DonationResponseDTO(
    UUID id,
    OffsetDateTime donationDate,
    BigDecimal totalWeightKg,
    UUID donorId,
    UUID proofAttachmentId,
    Boolean isActive,
    List<InputItemResponseDTO> inputItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### DonorCreateRequestDTO

[src/main/java/org/code/api/dto/donor/request/DonorCreateRequestDTO.java](../src/main/java/org/code/api/dto/donor/request/DonorCreateRequestDTO.java)

~~~java
public record DonorCreateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @NotBlank(message = "Document is required")
    @Size(max = 20, message = "Document must be at most 20 characters")
    String document,
    @NotNull(message = "Donor type is required")
    DonorType donorType
) {}
~~~

### DonorUpdateRequestDTO

[src/main/java/org/code/api/dto/donor/request/DonorUpdateRequestDTO.java](../src/main/java/org/code/api/dto/donor/request/DonorUpdateRequestDTO.java)

~~~java
public record DonorUpdateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @Size(max = 20, message = "Document must be at most 20 characters")
    String document
) {}
~~~

### DonorResponseDTO

[src/main/java/org/code/api/dto/donor/response/DonorResponseDTO.java](../src/main/java/org/code/api/dto/donor/response/DonorResponseDTO.java)

~~~java
public record DonorResponseDTO(
    UUID id,
    String name,
    String document,
    DonorType donorType,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### InventoryBalanceResponseDTO

[src/main/java/org/code/api/dto/inventory/response/InventoryBalanceResponseDTO.java](../src/main/java/org/code/api/dto/inventory/response/InventoryBalanceResponseDTO.java)

~~~java
public record InventoryBalanceResponseDTO(
    UUID id,
    UUID materialSubtypeId,
    BigDecimal currentWeightKg,
    BigDecimal currentVolumeM3,
    OffsetDateTime lastUpdatedAt
) {}
~~~

### InventoryLogResponseDTO

[src/main/java/org/code/api/dto/inventory/response/InventoryLogResponseDTO.java](../src/main/java/org/code/api/dto/inventory/response/InventoryLogResponseDTO.java)

~~~java
public record InventoryLogResponseDTO(
    UUID id,
    UUID materialSubtypeId,
    BigDecimal quantityKg,
    BigDecimal quantityM3,
    OperationType operationType,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### VehicleBulkCreateRequestDTO

[src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkCreateRequestDTO.java](../src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkCreateRequestDTO.java)

~~~java
public record VehicleBulkCreateRequestDTO(
    @NotEmpty(message = "Vehicle list must not be empty")
    @Size(max = 100, message = "Cannot create more than 100 vehicles at once")
    @Valid
    List<VehicleCreateRequestDTO> vehicles
) {}
~~~

### VehicleBulkUpdateItemDTO

[src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkUpdateItemDTO.java](../src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkUpdateItemDTO.java)

~~~java
public record VehicleBulkUpdateItemDTO(
    @NotNull(message = "Vehicle ID is required")
    UUID id,
    @NotBlank(message = "License plate is required")
    @Size(max = 20, message = "License plate must be at most 20 characters")
    String licensePlate,
    @Size(max = 100, message = "Model must be at most 100 characters")
    String model,
    @NotNull(message = "Active status is required")
    Boolean isActive
) {}
~~~

### VehicleBulkUpdateRequestDTO

[src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkUpdateRequestDTO.java](../src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkUpdateRequestDTO.java)

~~~java
public record VehicleBulkUpdateRequestDTO(
    @NotEmpty(message = "Vehicle list must not be empty")
    @Size(max = 100, message = "Cannot update more than 100 vehicles at once")
    @Valid
    List<VehicleBulkUpdateItemDTO> vehicles
) {}
~~~

### VehicleCreateRequestDTO

[src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleCreateRequestDTO.java](../src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleCreateRequestDTO.java)

~~~java
public record VehicleCreateRequestDTO(
    @NotBlank(message = "License plate is required")
    @Size(max = 20, message = "License plate must be at most 20 characters")
    String licensePlate,
    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must be at most 100 characters")
    String model
) {}
~~~

### VehicleUpdateRequestDTO

[src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleUpdateRequestDTO.java](../src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleUpdateRequestDTO.java)

~~~java
public record VehicleUpdateRequestDTO(
    @NotBlank(message = "License plate is required")
    @Size(max = 20, message = "License plate must be at most 20 characters")
    String licensePlate,
    @Size(max = 100, message = "Model must be at most 100 characters")
    String model,
    @NotNull(message = "Active status is required")
    Boolean isActive
) {}
~~~

### VehicleResponseDTO

[src/main/java/org/code/api/dto/logistic/vehicle/response/VehicleResponseDTO.java](../src/main/java/org/code/api/dto/logistic/vehicle/response/VehicleResponseDTO.java)

~~~java
public record VehicleResponseDTO(
    UUID id,
    String licensePlate,
    String model,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String creatorId
) {}
~~~

### MaterialCategoryCreateRequestDTO

[src/main/java/org/code/api/dto/material/request/MaterialCategoryCreateRequestDTO.java](../src/main/java/org/code/api/dto/material/request/MaterialCategoryCreateRequestDTO.java)

~~~java
public record MaterialCategoryCreateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name
) {}
~~~

### MaterialCategoryUpdateRequestDTO

[src/main/java/org/code/api/dto/material/request/MaterialCategoryUpdateRequestDTO.java](../src/main/java/org/code/api/dto/material/request/MaterialCategoryUpdateRequestDTO.java)

~~~java
public record MaterialCategoryUpdateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {}
~~~

### MaterialSubtypeCreateRequestDTO

[src/main/java/org/code/api/dto/material/request/MaterialSubtypeCreateRequestDTO.java](../src/main/java/org/code/api/dto/material/request/MaterialSubtypeCreateRequestDTO.java)

~~~java
public record MaterialSubtypeCreateRequestDTO(
    @NotNull(message = "Type ID is required")
    UUID typeId,
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name
) {}
~~~

### MaterialSubtypeUpdateRequestDTO

[src/main/java/org/code/api/dto/material/request/MaterialSubtypeUpdateRequestDTO.java](../src/main/java/org/code/api/dto/material/request/MaterialSubtypeUpdateRequestDTO.java)

~~~java
public record MaterialSubtypeUpdateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {}
~~~

### MaterialTypeCreateRequestDTO

[src/main/java/org/code/api/dto/material/request/MaterialTypeCreateRequestDTO.java](../src/main/java/org/code/api/dto/material/request/MaterialTypeCreateRequestDTO.java)

~~~java
public record MaterialTypeCreateRequestDTO(
    @NotNull(message = "Category ID is required")
    UUID categoryId,
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name
) {}
~~~

### MaterialTypeUpdateRequestDTO

[src/main/java/org/code/api/dto/material/request/MaterialTypeUpdateRequestDTO.java](../src/main/java/org/code/api/dto/material/request/MaterialTypeUpdateRequestDTO.java)

~~~java
public record MaterialTypeUpdateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {}
~~~

### MaterialCategoryResponseDTO

[src/main/java/org/code/api/dto/material/response/MaterialCategoryResponseDTO.java](../src/main/java/org/code/api/dto/material/response/MaterialCategoryResponseDTO.java)

~~~java
public record MaterialCategoryResponseDTO(
    UUID id,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### MaterialSubtypeResponseDTO

[src/main/java/org/code/api/dto/material/response/MaterialSubtypeResponseDTO.java](../src/main/java/org/code/api/dto/material/response/MaterialSubtypeResponseDTO.java)

~~~java
public record MaterialSubtypeResponseDTO(
    UUID id,
    UUID typeId,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### MaterialTypeResponseDTO

[src/main/java/org/code/api/dto/material/response/MaterialTypeResponseDTO.java](../src/main/java/org/code/api/dto/material/response/MaterialTypeResponseDTO.java)

~~~java
public record MaterialTypeResponseDTO(
    UUID id,
    UUID categoryId,
    String name,
    Boolean isActive,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### PressedBaleRequestDTO

[src/main/java/org/code/api/dto/pressing/request/PressedBaleRequestDTO.java](../src/main/java/org/code/api/dto/pressing/request/PressedBaleRequestDTO.java)

~~~java
public record PressedBaleRequestDTO(
    UUID sortedItemId,
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    BigDecimal weightKg,
    @NotNull(message = "Initial volume is required")
    @Positive(message = "Initial volume must be positive")
    BigDecimal initialVolumeM3,
    @NotNull(message = "Final volume is required")
    @Positive(message = "Final volume must be positive")
    BigDecimal finalVolumeM3,
    DestinationType destinationType,
    UUID destinationId
) {}
~~~

### PressingCreateRequestDTO

[src/main/java/org/code/api/dto/pressing/request/PressingCreateRequestDTO.java](../src/main/java/org/code/api/dto/pressing/request/PressingCreateRequestDTO.java)

~~~java
public record PressingCreateRequestDTO(
    OffsetDateTime pressingDate,
    @NotEmpty(message = "This list must not be empty")
    @Valid
    List<PressedBaleRequestDTO> pressedBales
) {}
~~~

### PressedBaleResponseDTO

[src/main/java/org/code/api/dto/pressing/response/PressedBaleResponseDTO.java](../src/main/java/org/code/api/dto/pressing/response/PressedBaleResponseDTO.java)

~~~java
public record PressedBaleResponseDTO(
    UUID id,
    UUID pressingId,
    UUID sortedItemId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal initialVolumeM3,
    BigDecimal finalVolumeM3,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DestinationType destinationType,
    UUID destinationId
) {}
~~~

### PressingResponseDTO

[src/main/java/org/code/api/dto/pressing/response/PressingResponseDTO.java](../src/main/java/org/code/api/dto/pressing/response/PressingResponseDTO.java)

~~~java
public record PressingResponseDTO(
    UUID id,
    OffsetDateTime pressingDate,
    Boolean isActive,
    List<PressedBaleResponseDTO> pressedBales,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### BuyerCreateRequestDTO

[src/main/java/org/code/api/dto/sale/request/BuyerCreateRequestDTO.java](../src/main/java/org/code/api/dto/sale/request/BuyerCreateRequestDTO.java)

~~~java
public record BuyerCreateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @Size(max = 20, message = "Document must be at most 20 characters")
    String document
) {}
~~~

### BuyerUpdateRequestDTO

[src/main/java/org/code/api/dto/sale/request/BuyerUpdateRequestDTO.java](../src/main/java/org/code/api/dto/sale/request/BuyerUpdateRequestDTO.java)

~~~java
public record BuyerUpdateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @Size(max = 20, message = "Document must be at most 20 characters")
    String document
) {}
~~~

### SaleCreateRequestDTO

[src/main/java/org/code/api/dto/sale/request/SaleCreateRequestDTO.java](../src/main/java/org/code/api/dto/sale/request/SaleCreateRequestDTO.java)

~~~java
public record SaleCreateRequestDTO(
    @NotNull(message = "Sale date is required")
    OffsetDateTime saleDate,
    @NotNull(message = "Buyer ID is required")
    UUID buyerId,
    UUID nfeAttachmentId,
    UUID mtrAttachmentId,
    UUID cdfAttachmentId,
    @NotNull(message = "Total value is required")
    @Positive(message = "Total value must be positive")
    BigDecimal totalValue,
    @Valid
    List<SaleItemRequestDTO> saleItems
) {}
~~~

### SaleItemRequestDTO

[src/main/java/org/code/api/dto/sale/request/SaleItemRequestDTO.java](../src/main/java/org/code/api/dto/sale/request/SaleItemRequestDTO.java)

~~~java
public record SaleItemRequestDTO(
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    BigDecimal weightKg,
    @NotNull(message = "Volume is required")
    @Positive(message = "Volume must be positive")
    BigDecimal volumeM3,
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    BigDecimal unitPrice
) {}
~~~

### BuyerResponseDTO

[src/main/java/org/code/api/dto/sale/response/BuyerResponseDTO.java](../src/main/java/org/code/api/dto/sale/response/BuyerResponseDTO.java)

~~~java
public record BuyerResponseDTO(
    UUID id,
    String name,
    String document,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### SaleItemResponseDTO

[src/main/java/org/code/api/dto/sale/response/SaleItemResponseDTO.java](../src/main/java/org/code/api/dto/sale/response/SaleItemResponseDTO.java)

~~~java
public record SaleItemResponseDTO(
    UUID id,
    UUID saleId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal volumeM3,
    BigDecimal unitPrice,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### SaleResponseDTO

[src/main/java/org/code/api/dto/sale/response/SaleResponseDTO.java](../src/main/java/org/code/api/dto/sale/response/SaleResponseDTO.java)

~~~java
public record SaleResponseDTO(
    UUID id,
    OffsetDateTime saleDate,
    UUID buyerId,
    UUID nfeAttachmentId,
    UUID mtrAttachmentId,
    UUID cdfAttachmentId,
    BigDecimal totalValue,
    Boolean isActive,
    List<SaleItemResponseDTO> saleItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### LoginRequestDTO

[src/main/java/org/code/api/dto/session/request/LoginRequestDTO.java](../src/main/java/org/code/api/dto/session/request/LoginRequestDTO.java)

~~~java
public record LoginRequestDTO(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    String email,
    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be at most 72 characters")
    String password
) {}
~~~

### RegisterRequestDTO

[src/main/java/org/code/api/dto/session/request/RegisterRequestDTO.java](../src/main/java/org/code/api/dto/session/request/RegisterRequestDTO.java)

~~~java
public record RegisterRequestDTO(
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must be at most 255 characters")
    String fullName,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    String email,
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    String password
) {}
~~~

### LoginResponseDTO

[src/main/java/org/code/api/dto/session/response/LoginResponseDTO.java](../src/main/java/org/code/api/dto/session/response/LoginResponseDTO.java)

~~~java
public record LoginResponseDTO(
    String token
) {}
~~~

### RegisterResponseDTO

[src/main/java/org/code/api/dto/session/response/RegisterResponseDTO.java](../src/main/java/org/code/api/dto/session/response/RegisterResponseDTO.java)

~~~java
public record RegisterResponseDTO(
    String token
) {}
~~~

### SortedItemRequestDTO

[src/main/java/org/code/api/dto/sorting/request/SortedItemRequestDTO.java](../src/main/java/org/code/api/dto/sorting/request/SortedItemRequestDTO.java)

~~~java
public record SortedItemRequestDTO(
    UUID inputItemId,
    @NotNull(message = "Material subtype ID is required")
    UUID materialSubtypeId,
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    BigDecimal weightKg,
    @NotNull(message = "Volume is required")
    @Positive(message = "Volume must be positive")
    BigDecimal volumeM3,
    @PositiveOrZero(message = "Reject weight must be zero or positive")
    BigDecimal rejectWeightKg,
    @PositiveOrZero(message = "Reject volume must be zero or positive")
    BigDecimal rejectVolumeM3,
    DestinationType destinationType,
    UUID destinationId
) {
    @AssertTrue(message = "rejectWeightKg must not exceed weightKg")
    public boolean isRejectWeightValid() {
        return rejectWeightKg == null || weightKg == null
            || rejectWeightKg.compareTo(weightKg) <= 0;
    }

    @AssertTrue(message = "rejectVolumeM3 must not exceed volumeM3")
    public boolean isRejectVolumeValid() {
        return rejectVolumeM3 == null || volumeM3 == null
            || rejectVolumeM3.compareTo(volumeM3) <= 0;
    }
}
~~~

### SortingCreateRequestDTO

[src/main/java/org/code/api/dto/sorting/request/SortingCreateRequestDTO.java](../src/main/java/org/code/api/dto/sorting/request/SortingCreateRequestDTO.java)

~~~java
public record SortingCreateRequestDTO(
    OffsetDateTime sortingDate,
    @NotNull(message = "Sorting type is required")
    SortingType sortingType,
    @NotEmpty(message = "The list must have a sorted item")
    @Valid
    List<SortedItemRequestDTO> sortedItems
) {}
~~~

### SortedItemResponseDTO

[src/main/java/org/code/api/dto/sorting/response/SortedItemResponseDTO.java](../src/main/java/org/code/api/dto/sorting/response/SortedItemResponseDTO.java)

~~~java
public record SortedItemResponseDTO(
    UUID id,
    UUID sortingId,
    UUID inputItemId,
    UUID materialSubtypeId,
    BigDecimal weightKg,
    BigDecimal volumeM3,
    BigDecimal rejectWeightKg,
    BigDecimal rejectVolumeM3,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DestinationType destinationType,
    UUID destinationId
) {}
~~~

### SortingResponseDTO

[src/main/java/org/code/api/dto/sorting/response/SortingResponseDTO.java](../src/main/java/org/code/api/dto/sorting/response/SortingResponseDTO.java)

~~~java
public record SortingResponseDTO(
    UUID id,
    OffsetDateTime sortingDate,
    SortingType sortingType,
    Boolean isActive,
    List<SortedItemResponseDTO> sortedItems,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

### TeamMemberCreateRequestDTO

[src/main/java/org/code/api/dto/team/request/TeamMemberCreateRequestDTO.java](../src/main/java/org/code/api/dto/team/request/TeamMemberCreateRequestDTO.java)

~~~java
public record TeamMemberCreateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @Size(max = 50, message = "Role must be at most 50 characters")
    String role
) {}
~~~

### TeamMemberUpdateRequestDTO

[src/main/java/org/code/api/dto/team/request/TeamMemberUpdateRequestDTO.java](../src/main/java/org/code/api/dto/team/request/TeamMemberUpdateRequestDTO.java)

~~~java
public record TeamMemberUpdateRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,
    @Size(max = 50, message = "Role must be at most 50 characters")
    String role
) {}
~~~

### TeamMemberResponseDTO

[src/main/java/org/code/api/dto/team/response/TeamMemberResponseDTO.java](../src/main/java/org/code/api/dto/team/response/TeamMemberResponseDTO.java)

~~~java
public record TeamMemberResponseDTO(
    UUID id,
    String name,
    String role,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
~~~

## Current identity and attachment additions

- POST /api/users: ADMINISTRATOR; safe UserResponse, no token.
- POST /api/documents: AttachmentResponse DTO, never a JPA entity.
- GET /api/documents/{id}/download: creator only.
- DELETE /api/documents/{id}: creator only; 202 queued deletion; 409 if referenced.
- The generated Swagger/OpenAPI document is authoritative for current request/response schemas.

## Organization and catalog update (2026-09-27)

Swagger now includes organization creation, current membership list/read and administrator grant/revoke endpoints from OrganizationController. Catalog lists apply creator/parent/name filters before paging and counting; updates require the last returned version and return 409 for stale submissions. These fixes do not change legacy creator ownership into organization ownership. Redis has no public HTTP endpoint; its connectivity contributes to readiness. See ADR-0009 and ADR-0010 for transition rules.
