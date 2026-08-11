package org.code.api.dto.sorting.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.code.api.domain.enums.DestinationType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SortedItemRequestDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void teardown() {
        factory.close();
    }

    @Test
    void should_pass_when_reject_is_zero() {
        var dto = dto(BigDecimal.valueOf(100), BigDecimal.valueOf(10),
                      BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void should_pass_when_reject_is_less_than_gross() {
        var dto = dto(BigDecimal.valueOf(100), BigDecimal.valueOf(10),
                      BigDecimal.valueOf(20), BigDecimal.valueOf(2));
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void should_pass_when_reject_equals_gross() {
        var dto = dto(BigDecimal.valueOf(100), BigDecimal.valueOf(10),
                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void should_fail_when_reject_weight_exceeds_gross_weight() {
        var dto = dto(BigDecimal.valueOf(100), BigDecimal.valueOf(10),
                      BigDecimal.valueOf(150), BigDecimal.ZERO);
        Set<ConstraintViolation<SortedItemRequestDTO>> violations = validator.validate(dto);
        assertThat(violations)
            .extracting(v -> v.getMessage())
            .contains("rejectWeightKg must not exceed weightKg");
    }

    @Test
    void should_fail_when_reject_volume_exceeds_gross_volume() {
        var dto = dto(BigDecimal.valueOf(100), BigDecimal.valueOf(10),
                      BigDecimal.ZERO, BigDecimal.valueOf(15));
        Set<ConstraintViolation<SortedItemRequestDTO>> violations = validator.validate(dto);
        assertThat(violations)
            .extracting(v -> v.getMessage())
            .contains("rejectVolumeM3 must not exceed volumeM3");
    }

    private static SortedItemRequestDTO dto(BigDecimal weight, BigDecimal volume,
                                            BigDecimal rejectWeight, BigDecimal rejectVolume) {
        return new SortedItemRequestDTO(
            null,
            UUID.randomUUID(),
            weight,
            volume,
            rejectWeight,
            rejectVolume,
            DestinationType.STOCK,
            null
        );
    }
}
