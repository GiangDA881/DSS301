package g2.g2_gp_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataImportResult {
    private boolean success;
    private String message;
    private int rawRecordsLoaded;
    private int customersProcessed;
    private int productsProcessed;
    private int ordersProcessed;
    private int orderItemsProcessed;
    private int errorCount;
    private String fileName;
}

