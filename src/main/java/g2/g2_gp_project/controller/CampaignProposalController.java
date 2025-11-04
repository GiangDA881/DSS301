package g2.g2_gp_project.controller;

import g2.g2_gp_project.dto.CampaignRequestDTO;
import g2.g2_gp_project.dto.ApiResponseDTO;
import g2.g2_gp_project.service.CampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/campaign")
public class CampaignProposalController {

    @Autowired
    private CampaignService campaignService;

    // 1. ENDPOINT TRẢ VỀ TRANG HTML (DÙNG THYMELEAF)
    @GetMapping("/proposal")
    public String showProposalPage(Model model) {
        // Trả về tên file: /resources/templates/campaign_proposal.html
        return "campaign_proposal";
    }

    // 2. ENDPOINT LẤY DANH SÁCH SEGMENTS TỪ DATABASE
    @GetMapping("/api/segments")
    @ResponseBody
    public ResponseEntity<List<String>> getAvailableSegments() {
        try {
            List<String> segments = campaignService.getAvailableSegments();
            return ResponseEntity.ok(segments);
        } catch (Exception e) {
            System.err.println("Error getting segments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 3. ENDPOINT API ĐỂ JAVASCRIPT GỌI (XỬ LÝ)
    @PostMapping("/api/propose")
    @ResponseBody // Trả về JSON, không phải HTML
    public ResponseEntity<ApiResponseDTO.RecommendedPlan> runProposal(
            @RequestBody CampaignRequestDTO requestDTO) {

        try {
            ApiResponseDTO.RecommendedPlan result = 
                campaignService.proposeCampaign(requestDTO);
            return ResponseEntity.ok(result); // Trả về 200 OK + JSON kết quả

        } catch (Exception e) {
            // Nếu gọi API Python thất bại
            System.err.println("Error proposing campaign: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
