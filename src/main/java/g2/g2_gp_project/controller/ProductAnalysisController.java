package g2.g2_gp_project.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import g2.g2_gp_project.dto.ProductAnalysisRequest;
import g2.g2_gp_project.service.ProductAnalysisService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductAnalysisController {

    private final ProductAnalysisService productAnalysisService;

    @PostMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> analyzeProducts(@RequestBody ProductAnalysisRequest request) {
        return ResponseEntity.ok(productAnalysisService.analyzeProducts(request));
    }

    @GetMapping("/countries")
    public ResponseEntity<List<String>> getCountries() {
        return ResponseEntity.ok(productAnalysisService.getAllCountries());
    }

    @PostMapping("/countries/compare")
    public ResponseEntity<List<Map<String, Object>>> compareCountries(@RequestBody ProductAnalysisRequest request) {
        return ResponseEntity.ok(productAnalysisService.compareCountriesByRevenue(request));
    }
}