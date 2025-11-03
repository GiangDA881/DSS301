package g2.g2_gp_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataQualityReport {
    private String fileName;
    private long totalRows;
    private long validRows;
    private long invalidRows;

    // Column-specific issues
    private Map<String, ColumnQuality> columnQuality = new HashMap<>();

    // Specific issues
    private List<DataIssue> issues = new ArrayList<>();

    // Summary statistics
    private long missingCustomerIds;
    private long invalidDates;
    private long invalidPrices;
    private long invalidQuantities;
    private long emptyDescriptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnQuality {
        private String columnName;
        private long totalValues;
        private long nullValues;
        private long emptyValues;
        private long invalidValues;
        private double qualityScore; // 0-100
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataIssue {
        private long rowNumber;
        private String columnName;
        private String issueType;
        private String value;
        private String description;
    }
}

