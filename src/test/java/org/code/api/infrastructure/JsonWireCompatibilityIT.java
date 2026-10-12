package org.code.api.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.code.api.domain.enums.SortingType;
import org.code.api.dto.sorting.request.SortedItemRequestDTO;
import org.code.api.dto.sorting.request.SortingCreateRequestDTO;
import org.code.api.dto.sorting.response.SortingResponseDTO;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Protects pre-upgrade command hash bytes and persisted response readability across Jackson
 * versions. The legacy mapper is an independent test oracle, never an application bean.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class JsonWireCompatibilityIT extends PostgresIntegrationTest {
  @Autowired ObjectMapper json;

  /**
   * Actual command DTO bytes must retain dates, decimal scale, property order and optional nulls.
   */
  @Test
  void preservesLegacySortingPayloadSerialization() throws Exception {
    var decimalModule =
        new SimpleModule()
            .addSerializer(
                BigDecimal.class,
                new JsonSerializer<BigDecimal>() {
                  @Override
                  public void serialize(
                      BigDecimal value, JsonGenerator generator, SerializerProvider provider)
                      throws IOException {
                    generator.writeString(value.toPlainString());
                  }
                });
    var legacy =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(decimalModule)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var payload =
        new SortingCreateRequestDTO(
            OffsetDateTime.parse("2026-10-08T23:59:59.123-03:00"),
            SortingType.PRIMARY,
            List.of(
                new SortedItemRequestDTO(
                    UUID.fromString("11111111-1111-4111-8111-111111111111"),
                    UUID.fromString("22222222-2222-4222-8222-222222222222"),
                    new BigDecimal("123.4500"),
                    new BigDecimal("0.0001"),
                    BigDecimal.ZERO,
                    null,
                    null,
                    null)));
    assertThat(json.writeValueAsString(payload)).isEqualTo(legacy.writeValueAsString(payload));
  }

  /**
   * Existing JSONB receipts retain their original offset and nullable update timestamp on replay.
   */
  @Test
  void readsPreUpgradePersistedReceiptWithoutChangingItsOffset() throws Exception {
    String historical =
        """
        {"id":"11111111-1111-4111-8111-111111111111",
         "sortingDate":"2026-10-08T09:30:00-03:00","sortingType":"PRIMARY",
         "isActive":true,"sortedItems":[],"createdAt":"2026-10-08T10:00:00-03:00",
         "updatedAt":null,"status":"POSTED"}
        """;
    SortingResponseDTO receipt =
        json.readerFor(SortingResponseDTO.class)
            .without(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .readValue(historical);
    assertThat(receipt.sortingDate().toString()).isEqualTo("2026-10-08T09:30-03:00");
    assertThat(receipt.createdAt().toString()).isEqualTo("2026-10-08T10:00-03:00");
    assertThat(receipt.updatedAt()).isNull();
    assertThat(receipt.sortedItems()).isEmpty();
    assertThat(receipt.status()).isEqualTo("POSTED");
  }
}
