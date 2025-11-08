package g2.g2_gp_project.controller;

import g2.g2_gp_project.dto.RetailSummary;
import g2.g2_gp_project.service.RetailForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class RetailForecastController {

    @Autowired
    private RetailForecastService forecastService;

    @GetMapping("/monthly-category-revenue")
    public List<RetailSummary> getMonthlyCategoryRevenue() {
        return forecastService.getMonthlyCategoryRevenue();
    }
}

