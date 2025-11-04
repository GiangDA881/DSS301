package g2.g2_gp_project.dto;

import java.util.Date;

/**
 * DTO Interface để nhận kết quả từ Native Query tính RFM
 * Các tên method phải khớp chính xác với tên cột (AS ...) trong SQL Query
 */
public interface RfmResultDTO {
    String getCustomerId();
    Date getLastPurchaseDate();
    Integer getFrequency();
    Double getMonetary();
}
