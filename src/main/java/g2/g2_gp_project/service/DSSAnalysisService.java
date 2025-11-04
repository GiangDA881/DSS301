package g2.g2_gp_project.service;

import g2.g2_gp_project.dto.RfmResultDTO;
import g2.g2_gp_project.entity.CustomerPrediction;
import g2.g2_gp_project.repository.CustomerPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service thực hiện phân tích DSS: Tính RFM, gán segment, và lưu dự báo
 */
@Service
public class DSSAnalysisService {

    @Autowired
    private CustomerPredictionRepository customerPredictionRepo;

    /**
     * Phân tích và lưu dự báo khách hàng
     * Pipeline: Mô tả (RFM) → Dự báo (Segment) → Load (Save)
     * 
     * @return Số lượng bản ghi đã lưu
     */
    @Transactional
    public int analyzeAndSavePredictions() {
        System.out.println("🔄 Bắt đầu phân tích RFM...");
        
        // ========================================
        // BƯỚC A: MÔ TÀ - Tính toán RFM từ OLTP
        // ========================================
        List<RfmResultDTO> rfmResults = customerPredictionRepo.calculateRfm();
        System.out.println("✅ Đã tính RFM cho " + rfmResults.size() + " khách hàng");
        
        if (rfmResults.isEmpty()) {
            System.out.println("⚠️ Không có dữ liệu để phân tích!");
            return 0;
        }
        
        // ========================================
        // BƯỚC B: DỰ BÁO - Gán nhãn Segment
        // ========================================
        Date analysisDate = new Date(); // Ngày hiện tại
        List<CustomerPrediction> predictionsToSave = new ArrayList<>();
        
        for (RfmResultDTO rfm : rfmResults) {
            // Tính Recency (số ngày từ lần mua cuối đến nay)
            long diffInMillies = analysisDate.getTime() - rfm.getLastPurchaseDate().getTime();
            int recency = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            
            Integer frequency = rfm.getFrequency();
            Double monetary = rfm.getMonetary();
            
            // Tạo đối tượng CustomerPrediction
            CustomerPrediction prediction = new CustomerPrediction();
            prediction.setCustomerId(rfm.getCustomerId());
            prediction.setRecency(recency);
            prediction.setFrequency(frequency);
            prediction.setMonetary(monetary);
            prediction.setPredictionDate(analysisDate);
            
            // ========================================
            // LOGIC DỰ BÁO: Gán Segment dựa trên RFM
            // ========================================
            String segment = determineSegment(recency, frequency, monetary);
            prediction.setSegment(segment);
            
            // Tính Repurchase Probability (ví dụ đơn giản)
            double repurchaseProbability = calculateRepurchaseProbability(recency, frequency, monetary);
            prediction.setRepurchaseProbability(repurchaseProbability);
            
            predictionsToSave.add(prediction);
        }
        
        System.out.println("✅ Đã gán segment cho " + predictionsToSave.size() + " khách hàng");
        
        // ========================================
        // BƯỚC C: LOAD - Xóa dữ liệu cũ và lưu mới
        // ========================================
        System.out.println("🗑️ Xóa dữ liệu dự báo cũ...");
        customerPredictionRepo.deleteAllInBatch();
        
        System.out.println("💾 Lưu dữ liệu dự báo mới...");
        customerPredictionRepo.saveAll(predictionsToSave);
        
        // ========================================
        // BƯỚC D: TRẢ VỀ KẾT QUẢ
        // ========================================
        int savedCount = predictionsToSave.size();
        System.out.println("✅ Hoàn thành! Đã lưu " + savedCount + " bản ghi vào customer_predictions");
        
        return savedCount;
    }
    
    /**
     * Logic gán Segment dựa trên RFM
     * Áp dụng mô hình phân khúc khách hàng chuẩn
     */
    private String determineSegment(int recency, int frequency, double monetary) {
        // Định nghĩa ngưỡng (threshold)
        final int RECENT_THRESHOLD = 30;      // Trong vòng 30 ngày
        final int MODERATE_RECENCY = 90;      // Trong vòng 90 ngày
        final int HIGH_FREQUENCY = 10;        // Mua >= 10 lần
        final int MODERATE_FREQUENCY = 5;     // Mua >= 5 lần
        final double HIGH_MONETARY = 1000.0;  // Chi tiêu >= $1000
        final double MODERATE_MONETARY = 500.0; // Chi tiêu >= $500
        
        // Champions: Mua gần đây, thường xuyên, chi nhiều
        if (recency <= RECENT_THRESHOLD && frequency >= HIGH_FREQUENCY && monetary >= HIGH_MONETARY) {
            return "Champions";
        }
        
        // Loyal: Mua thường xuyên, chi nhiều (không nhất thiết gần đây)
        if (frequency >= HIGH_FREQUENCY && monetary >= HIGH_MONETARY) {
            return "Loyal";
        }
        
        // Potential Loyalist: Mua gần đây, tần suất trung bình
        if (recency <= MODERATE_RECENCY && frequency >= MODERATE_FREQUENCY && monetary >= MODERATE_MONETARY) {
            return "Potential Loyalist";
        }
        
        // At Risk: Đã từng mua nhiều nhưng không mua gần đây
        if (recency > MODERATE_RECENCY && recency <= 180 && frequency >= MODERATE_FREQUENCY) {
            return "At Risk";
        }
        
        // Hibernating: Lâu không mua, tần suất thấp
        if (recency > 180 && recency <= 365 && frequency < MODERATE_FREQUENCY) {
            return "Hibernating";
        }
        
        // Lost: Rất lâu không mua (> 1 năm)
        if (recency > 365) {
            return "Lost";
        }
        
        // New Customers: Mua gần đây nhưng tần suất thấp
        if (recency <= RECENT_THRESHOLD && frequency < MODERATE_FREQUENCY) {
            return "New Customers";
        }
        
        // Promising: Mua gần đây, chi tiêu trung bình
        if (recency <= MODERATE_RECENCY && monetary >= MODERATE_MONETARY) {
            return "Promising";
        }
        
        // Default: About To Sleep (Sắp ngủ đông)
        return "About To Sleep";
    }
    
    /**
     * Tính xác suất mua lại (Repurchase Probability)
     * Công thức đơn giản: càng gần đây, tần suất cao, chi nhiều → xác suất cao
     */
    private double calculateRepurchaseProbability(int recency, int frequency, double monetary) {
        // Normalize các giá trị về [0, 1]
        double recencyScore = Math.max(0, 1 - (recency / 365.0)); // Càng gần = điểm cao
        double frequencyScore = Math.min(1, frequency / 20.0);     // Max 20 lần
        double monetaryScore = Math.min(1, monetary / 5000.0);     // Max $5000
        
        // Tính trung bình có trọng số
        double probability = (recencyScore * 0.5) + (frequencyScore * 0.3) + (monetaryScore * 0.2);
        
        // Làm tròn 2 chữ số thập phân
        return Math.round(probability * 100.0) / 100.0;
    }
}
