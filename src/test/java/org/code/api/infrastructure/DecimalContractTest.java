package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.UUID;
import org.code.api.dto.collection.request.InputItemRequestDTO;
import org.code.api.infrastructure.documentation.OpenApiConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Protects the exact decimal wire boundary and rejects persistence-time quantity rounding.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class DecimalContractTest {
  /** Scientific Java values must remain plain decimal strings in JSON. */
  @Test
  void serializesPlainStringsWithoutLosingScale() throws Exception {
    var builder = new Jackson2ObjectMapperBuilder();
    new OpenApiConfiguration().decimalWireFormat().customize(builder);
    var mapper = builder.build();
    assertThat(mapper.writeValueAsString(new BigDecimal("1E+10"))).isEqualTo("\"10000000000\"");
    assertThat(mapper.writeValueAsString(new BigDecimal("0.0000"))).isEqualTo("\"0.0000\"");
    assertThat(mapper.writeValueAsString(new BigDecimal("123.4500"))).isEqualTo("\"123.4500\"");
  }

  /** Values beyond NUMERIC(15,4) are rejected before the database can round them. */
  @Test
  void rejectsExcessScaleAndPrecisionOnRawIntake() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      assertThat(
              validator.validate(
                  new InputItemRequestDTO(
                      UUID.randomUUID(), new BigDecimal("1.00001"), new BigDecimal("1"))))
          .isNotEmpty();
      assertThat(
              validator.validate(
                  new InputItemRequestDTO(
                      UUID.randomUUID(), new BigDecimal("100000000000"), new BigDecimal("1"))))
          .isNotEmpty();
      assertThat(
              validator.validate(
                  new InputItemRequestDTO(
                      UUID.randomUUID(),
                      new BigDecimal("99999999999.9999"),
                      new BigDecimal("0.0001"))))
          .isEmpty();
    }
  }
}
