# Domain enum vocabulary

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Current values are source contracts. Renaming values can affect stored strings and JSON; coordinate SQL/backfill and frontend mappings before a change. SortingType stage semantics remain a decision gate in ADR-0004/0005.

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
