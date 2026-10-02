# Domain enum vocabulary

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Current values are source contracts. Renaming values can affect stored strings and JSON; coordinate SQL/backfill and frontend mappings before a change. SortingType is descriptive metadata; each sorting command independently allocates raw input under ADR-0012. It does not authorize repeated credits for the same raw quantities.

## DestinationType

[src/main/java/org/code/api/domain/enums/DestinationType.java](../src/main/java/org/code/api/domain/enums/DestinationType.java)

~~~java
public enum DestinationType {
    STOCK,
    SALE,
    PRESSING
}
~~~

## DonorType

[src/main/java/org/code/api/domain/enums/DonorType.java](../src/main/java/org/code/api/domain/enums/DonorType.java)

~~~java
public enum DonorType {
    PF,
    PJ
}
~~~

## OperationType

[src/main/java/org/code/api/domain/enums/OperationType.java](../src/main/java/org/code/api/domain/enums/OperationType.java)

~~~java
public enum OperationType {
    COLLECTION_INPUT,
    DONATION_INPUT,
    SALE_OUTPUT,
    SORTING_INPUT,
    SORTING_OUTPUT,
    PRESSING_INPUT,
    PRESSING_OUTPUT,
    MANUAL_ADJUSTMENT
}
~~~

## SortingType

[src/main/java/org/code/api/domain/enums/SortingType.java](../src/main/java/org/code/api/domain/enums/SortingType.java)

~~~java
public enum SortingType {
    GROSS,
    PRIMARY,
    FINE
}
~~~

## UserRole

[src/main/java/org/code/api/domain/enums/UserRole.java](../src/main/java/org/code/api/domain/enums/UserRole.java)

~~~java
public enum UserRole {
    ADMINISTRATOR,
    CITY_HALL,
    ORGANIZATION,
    REPRESENTATIVE
}
~~~

## Compatibility and lifecycle vocabulary

PF means an individual donor and PJ means a legal-entity donor. These existing stored/wire codes remain unchanged; all maintained explanations are English. DestinationType retains SALE/PRESSING for historical compatibility, but new sorting/pressing requests accept only STOCK without destinationId. Typed downstream commands create real links.

Membership roles are MEMBER and MANAGER, independent of platform UserRole. Team roles are DRIVER, HELPER and OPERATOR. Sales use DRAFT/POSTED/REVERSED; processing uses POSTED/REVERSED. Stock operations use SORTING/PRESSING/SALE/REVERSAL. These states are database-checked strings in their owning modules and are documented in Swagger and ADR-0012.
