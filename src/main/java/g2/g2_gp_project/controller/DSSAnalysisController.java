package g2.g2_gp_project.controller;

import g2.g2_gp_project.service.DSSAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller cho Marketing thực hiện phân tích RFM
 */
@Controller
@RequestMapping("/marketing/analysis")
@CrossOrigin(origins = "*")
public class DSSAnalysisController {

    @Autowired
    private DSSAnalysisService dssAnalysisService;

    /**
     * Hiển thị trang phân tích RFM
     * GET /marketing/analysis/rfm
     */
    @GetMapping("/rfm")
    public String showRfmAnalysisPage(Model model) {
        return "rfm_analysis";
    }

    /**
     * Endpoint API để chạy phân tích RFM và lưu dự báo
     * POST /marketing/analysis/api/run-rfm
     * 
     * @return Kết quả phân tích (số bản ghi đã lưu)
     */
    @PostMapping("/api/run-rfm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> runAnalysis() {
        try {
            System.out.println("📊 Nhận yêu cầu chạy phân tích RFM từ Admin...");
            
            long startTime = System.currentTimeMillis();
            
            // Gọi service thực hiện phân tích
            int recordsSaved = dssAnalysisService.analyzeAndSavePredictions();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Chuẩn bị response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Chạy phân tích RFM thành công!");
            response.put("recordsSaved", recordsSaved);
            response.put("durationMs", duration);
            response.put("durationSeconds", duration / 1000.0);
            
            System.out.println("✅ Hoàn thành phân tích trong " + (duration / 1000.0) + " giây");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi chạy phân tích: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi khi chạy phân tích: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Endpoint để kiểm tra status của hệ thống phân tích
     * GET /marketing/analysis/api/status
     */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "RFM Analysis Service");
        status.put("status", "READY");
        status.put("description", "Sẵn sàng chạy phân tích RFM cho Marketing");
        
        return ResponseEntity.ok(status);
    }
}
