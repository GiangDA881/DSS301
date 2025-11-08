package g2.g2_gp_project.controller;

import g2.g2_gp_project.dto.ApiResponse;
import g2.g2_gp_project.dto.DataImportResult;
import g2.g2_gp_project.dto.DataQualityReport;
import g2.g2_gp_project.service.DataImportService;
import g2.g2_gp_project.service.DataQualityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
@Slf4j
public class
DataJobController {

    private final DataImportService dataImportService;
    private final DataQualityService dataQualityService;

    /**
     * Analyze CSV/Excel file quality before importing
     * Only accessible by ADMIN role
     */
    @PostMapping("/analyze-file")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DataQualityReport>> analyzeFile(
            @RequestParam("file") MultipartFile file) {

        log.info("Received file analysis request: {}", file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File is empty"));
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File name is null"));
            }

            // Check file extension
            String lowerCaseFileName = fileName.toLowerCase();
            if (!lowerCaseFileName.endsWith(".csv") &&
                !lowerCaseFileName.endsWith(".xlsx") &&
                !lowerCaseFileName.endsWith(".xls")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid file format. Only CSV and Excel files are supported."));
            }

            // Analyze file quality
            DataQualityReport report = dataQualityService.analyzeFile(file);

            return ResponseEntity.ok(
                    ApiResponse.success("File analyzed successfully", report)
            );

        } catch (Exception e) {
            log.error("Error analyzing file: {}", e.getMessage(), e);
            return ResponseEntity.ok()
                    .body(ApiResponse.error("Error analyzing file: " + e.getMessage()));
        }
    }

    /**
     * Upload CSV/Excel file and process through ETL pipeline
     * Only accessible by ADMIN role
     */
    @PostMapping("/upload-csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DataImportResult>> uploadCsvFile(
            @RequestParam("file") MultipartFile file) {

        log.info("Received file upload request: {}", file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<DataImportResult>builder()
                                .success(false)
                                .message("File is empty")
                                .build());
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<DataImportResult>builder()
                                .success(false)
                                .message("File name is null")
                                .build());
            }

            // Check file extension
            String lowerCaseFileName = fileName.toLowerCase();
            if (!lowerCaseFileName.endsWith(".csv") &&
                !lowerCaseFileName.endsWith(".xlsx") &&
                !lowerCaseFileName.endsWith(".xls")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<DataImportResult>builder()
                                .success(false)
                                .message("Invalid file format. Only CSV and Excel files are supported.")
                                .build());
            }

            // Process file through ETL pipeline
            DataImportResult result = dataImportService.processAndLoadFile(file);

            if (result.isSuccess()) {
                return ResponseEntity.ok(
                        ApiResponse.<DataImportResult>builder()
                                .success(true)
                                .message("File processed successfully")
                                .data(result)
                                .build()
                );
            } else {
                // Return error but with 200 status so frontend can read the message
                return ResponseEntity.ok()
                        .body(ApiResponse.<DataImportResult>builder()
                                .success(false)
                                .message(result.getMessage())
                                .data(result)
                                .build());
            }

        } catch (Exception e) {
            log.error("Error processing file: {}", e.getMessage(), e);
            return ResponseEntity.ok()
                    .body(ApiResponse.<DataImportResult>builder()
                            .success(false)
                            .message("Error processing file: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Health check endpoint for data job service
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Data job service is running")
                        .data("OK")
                        .build()
        );
    }
}
