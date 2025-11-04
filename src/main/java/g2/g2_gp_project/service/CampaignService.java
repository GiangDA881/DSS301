package g2.g2_gp_project.service;

import g2.g2_gp_project.dto.*;
import g2.g2_gp_project.entity.CampaignProposal;
import g2.g2_gp_project.entity.CustomerPrediction;
import g2.g2_gp_project.repository.CampaignProposalRepository;
import g2.g2_gp_project.repository.CustomerPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CustomerPredictionRepository customerPredictionRepo;

    @Autowired
    private CampaignProposalRepository campaignProposalRepo;

    @Value("${python.api.url}/propose-campaign")
    private String pythonApiUrl;

    /**
     * Lấy danh sách các segment có sẵn từ database
     * @return Danh sách segment
     */
    public List<String> getAvailableSegments() {
        try {
            List<String> segments = customerPredictionRepo.findDistinctSegments();
            if (segments.isEmpty()) {
                // Nếu không có dữ liệu, trả về danh sách mặc định
                System.out.println("⚠️ Không có segments trong DB, trả về danh sách mặc định");
                return Arrays.asList(
                    "Champions",
                    "Loyal",
                    "Potential Loyalist",
                    "At Risk",
                    "Hibernating",
                    "Lost",
                    "New Customers",
                    "Promising",
                    "About To Sleep"
                );
            }
            return segments;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lấy segments: " + e.getMessage());
            // Trả về danh sách mặc định nếu có lỗi
            return Arrays.asList(
                "Champions",
                "Loyal",
                "At Risk",
                "Hibernating",
                "Lost"
            );
        }
    }

    /**
     * Tạo dữ liệu mẫu cho demo nếu không có dữ liệu thực trong DB
     */
    private List<ApiRequestBody.AudienceMember> generateSampleAudience(String segment, int count) {
        Random random = new Random();
        List<ApiRequestBody.AudienceMember> sampleAudience = new ArrayList<>();
        
        // Giá trị avg_revenue khác nhau theo segment
        double baseRevenue = switch (segment.toLowerCase()) {
            case "champions" -> 500.0;
            case "loyal" -> 300.0;
            case "at risk" -> 200.0;
            case "hibernating" -> 150.0;
            case "lost" -> 100.0;
            default -> 200.0;
        };
        
        for (int i = 0; i < count; i++) {
            String customerId = "SAMPLE_" + segment.toUpperCase().replace(" ", "_") + "_" + String.format("%03d", i + 1);
            double avgRevenue = baseRevenue + (random.nextDouble() * 100 - 50); // ±50
            sampleAudience.add(new ApiRequestBody.AudienceMember(customerId, Math.max(avgRevenue, 50)));
        }
        
        return sampleAudience;
    }

    public ApiResponseDTO.RecommendedPlan proposeCampaign(CampaignRequestDTO requestDTO) {

        // 1. Lấy dữ liệu từ Postgres
        List<CustomerPrediction> audienceData = 
            customerPredictionRepo.findBySegment(requestDTO.getTargetSegment());

        List<ApiRequestBody.AudienceMember> audienceForApi;
        
        if (audienceData.isEmpty()) {
            // ⚠️ KHÔNG CÓ DỮ LIỆU THỰC - SỬ DỤNG DỮ LIỆU MẪU
            System.out.println("⚠️ WARNING: Không tìm thấy dữ liệu cho segment '" + requestDTO.getTargetSegment() 
                + "' trong bảng customer_predictions. Sử dụng dữ liệu mẫu để demo.");
            
            // Tạo 50-100 khách hàng mẫu
            int sampleCount = 50 + new Random().nextInt(51);
            audienceForApi = generateSampleAudience(requestDTO.getTargetSegment(), sampleCount);
        } else {
            // 2. Chuyển đổi Entity sang DTO cho API
            System.out.println("✅ Tìm thấy " + audienceData.size() + " khách hàng cho segment: " + requestDTO.getTargetSegment());
            audienceForApi = audienceData.stream()
                .map(c -> new ApiRequestBody.AudienceMember(
                        c.getCustomerId(),
                        c.getMonetary() / (c.getFrequency() > 0 ? c.getFrequency() : 1) // Tính avg_revenue
                ))
                .collect(Collectors.toList());
        }

        // 3. Chuẩn bị Request Body để gọi Python
        ApiRequestBody apiRequestBody = new ApiRequestBody(
            requestDTO.getTotalBudget(),
            audienceForApi,
            requestDTO.getAvailableActions()
        );

        // 4. Gọi Python API
        ApiResponseDTO apiResponse = 
            restTemplate.postForObject(pythonApiUrl, apiRequestBody, ApiResponseDTO.class);

        if (apiResponse == null || apiResponse.getRecommendedPlan() == null) {
            throw new RuntimeException("API Python không trả về kết quả hợp lệ.");
        }

        ApiResponseDTO.RecommendedPlan plan = apiResponse.getRecommendedPlan();

        // 5. Lưu kết quả đề xuất vào Postgres
        CampaignProposal proposalLog = new CampaignProposal();
        proposalLog.setCampaignName(requestDTO.getCampaignName());
        proposalLog.setTargetSegment(requestDTO.getTargetSegment());
        proposalLog.setExpectedCost(plan.getTotalCost());
        proposalLog.setExpectedRevenue(plan.getRevenueSaved());
        proposalLog.setRoi(plan.getPredictedRoi());
        proposalLog.setCreatedAt(new Date());
        campaignProposalRepo.save(proposalLog);

        // 6. Trả kết quả về cho Controller
        return plan;
    }
}
