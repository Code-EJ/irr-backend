package org.code.api.reporting.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Exact backend ratios with explicit missing/zero-denominator semantics.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class AnalyticsMath {
  private AnalyticsMath() {}

  /** Rounds once after aggregation; missing or nonpositive denominators are unavailable. */
  public static BigDecimal ratio(
      BigDecimal numerator, BigDecimal denominator, BigDecimal multiplier) {
    if (numerator == null || denominator == null || denominator.signum() <= 0) return null;
    return numerator.multiply(multiplier).divide(denominator, 6, RoundingMode.HALF_UP);
  }
}
