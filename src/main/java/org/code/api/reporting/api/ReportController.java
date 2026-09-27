package org.code.api.reporting.api;
import java.time.LocalDate;
import org.code.api.reporting.application.OperationalReports;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 * Live organization reports in JSON and CSV; all period boundaries are inclusive UTC dates.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController @lombok.RequiredArgsConstructor @RequestMapping("/api/v1/reports")
public class ReportController {
    private final OperationalReports reports;
    /** Returns current-status operational totals and the current stock snapshot. */
    @GetMapping("/summary")
    public OperationalReports.Summary summary(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) { return reports.summary(from,to); }
    /** Exports exactly the same summary semantics with fixed English column headers. */
    @GetMapping(value="/summary.csv",produces="text/csv")
    public ResponseEntity<String> csv(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        var r=reports.summary(from,to);
        String header="from_utc,to_utc,collection_weight_kg,donation_weight_kg,sorted_weight_kg,rejected_weight_kg,pressed_weight_kg,sold_weight_kg,revenue_brl,current_stock_weight_kg,current_stock_volume_m3";
        String row=String.join(",",r.from().toString(),r.to().toString(),r.collectionWeightKg().toPlainString(),r.donationWeightKg().toPlainString(),r.sortedWeightKg().toPlainString(),r.rejectedWeightKg().toPlainString(),r.pressedWeightKg().toPlainString(),r.soldWeightKg().toPlainString(),r.revenueBrl().toPlainString(),r.currentStockWeightKg().toPlainString(),r.currentStockVolumeM3().toPlainString());
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename=irr-summary.csv").body(header+"\r\n"+row+"\r\n");
    }
}
