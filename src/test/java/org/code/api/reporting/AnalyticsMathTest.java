package org.code.api.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.code.api.reporting.application.AnalyticsMath;
import org.junit.jupiter.api.Test;

/**
 * Verifies exact ratio boundaries and rounding without frontend arithmetic.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class AnalyticsMathTest {
  /** Missing evidence and zero denominators cannot produce fabricated zero indicators. */
  @Test
  void unknownAndZeroDenominatorsAreUnavailable() {
    assertThat(AnalyticsMath.ratio(null, BigDecimal.ONE, BigDecimal.ONE)).isNull();
    assertThat(AnalyticsMath.ratio(BigDecimal.ONE, null, BigDecimal.ONE)).isNull();
    assertThat(AnalyticsMath.ratio(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE)).isNull();
  }

  /** Recorded zero numerator remains a valid zero and signs are preserved for deviations. */
  @Test
  void zeroAndNegativeNumeratorsRemainDefined() {
    assertThat(AnalyticsMath.ratio(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE))
        .isEqualByComparingTo("0");
    assertThat(AnalyticsMath.ratio(new BigDecimal("-2"), BigDecimal.TEN, new BigDecimal("100")))
        .isEqualByComparingTo("-20");
  }

  /** Precision is rounded once on the final weighted sum. */
  @Test
  void roundsFinalRatioHalfUp() {
    assertThat(AnalyticsMath.ratio(BigDecimal.ONE, new BigDecimal("6"), BigDecimal.ONE))
        .isEqualByComparingTo("0.166667");
    assertThat(
            AnalyticsMath.ratio(
                new BigDecimal("20"), new BigDecimal("5400"), new BigDecimal("3600")))
        .isEqualByComparingTo("13.333333");
  }
}
