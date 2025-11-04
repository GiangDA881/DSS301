package g2.g2_gp_project.controller;

import g2.g2_gp_project.service.SalesDashboardService;
import g2.g2_gp_project.service.SalesETLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")

public class SalesDashboardController {
    @Autowired
    private SalesDashboardService dashboardService;

    @Autowired
    private SalesETLService etlService;

    /**
     * Kích hoạt ETL thủ công
     */
    @PostMapping("/etl/run")
    public ResponseEntity<Map<String, String>> runETL() {
        try {
            etlService.executeETL();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "ETL completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Lấy tổng quan doanh số theo quốc gia
     */
    @GetMapping("/sales-by-country")
    public ResponseEntity<?> getSalesByCountry(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate) {

        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        return ResponseEntity.ok(dashboardService.getSalesByCountry(start, end));
    }

    /**
     * Lấy top sản phẩm bán chạy theo quốc gia
     */
    @GetMapping("/top-products/{country}")
    public ResponseEntity<?> getTopProductsByCountry(
            @PathVariable String country,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getTopProductsByCountry(country, limit));
    }

    /**
     * Lấy xu hướng doanh số theo tháng
     */
    @GetMapping("/sales-trend")
    public ResponseEntity<?> getSalesTrend(
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(dashboardService.getSalesTrend(country, months));
    }

    /**
     * Lấy so sánh hiệu suất giữa các quốc gia
     */
    @GetMapping("/country-comparison")
    public ResponseEntity<?> getCountryComparison() {
        return ResponseEntity.ok(dashboardService.getCountryPerformanceComparison());
    }

    /**
     * Lấy thống kê tổng quan
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        return ResponseEntity.ok(dashboardService.getDashboardOverview());
    }

    /**
     * Lấy phân tích theo danh mục sản phẩm
     */
    @GetMapping("/category-analysis")
    public ResponseEntity<?> getCategoryAnalysis(
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(dashboardService.getCategoryAnalysis(country));
    }

    /**
     * Lấy dữ liệu để vẽ bản đồ nhiệt (heatmap)
     */
    @GetMapping("/sales-heatmap")
    public ResponseEntity<?> getSalesHeatmap() {
        return ResponseEntity.ok(dashboardService.getSalesHeatmap());
    }
    @GetMapping("/country-details/{country}")
    public ResponseEntity<?> getCountryDetails(
            @PathVariable String country,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate) {

        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        return ResponseEntity.ok(dashboardService.getCountryDetailStats(country, start, end));
    }

    /**
     * Export dữ liệu phân tích
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportDashboardData(
            @RequestParam(defaultValue = "json") String format) {
        // Implementation cho CSV/Excel export
        return ResponseEntity.ok(Map.of(
                "message", "Export feature coming soon",
                "format", format
        ));
    }

}
