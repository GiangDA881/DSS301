package g2.g2_gp_project.controller;

import g2.g2_gp_project.service.CustomerValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer-value")
@CrossOrigin(origins = "*")
public class CustomerValueController {

    @Autowired
    private CustomerValueService service;

    @GetMapping("/analyze")
    public List<Map<String, Object>> analyze(
            @RequestParam(defaultValue = "All") String country,
            @RequestParam(defaultValue = "0") double threshold,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return service.analyzeCustomerValue(country, threshold, limit);
    }
}
