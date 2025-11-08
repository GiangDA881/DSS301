package g2.g2_gp_project.controller;

import g2.g2_gp_project.dto.RetailSummary;
import g2.g2_gp_project.service.RetailForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private RetailForecastService forecastService;

    @GetMapping("/revenue-forecast")
    public String showDashboard(Model model) {
        return "revenue_forecast";
    }

    @PostMapping("/revenue-forecast/analyze")
    public String analyze(
            @RequestParam(name = "category", defaultValue = "ALL") String selectedCategory,
            @RequestParam(name = "country", defaultValue = "ALL") String selectedCountry,
            @RequestParam(name = "forecastMonths", defaultValue = "3") int forecastMonths,
            @RequestParam(name = "topN", defaultValue = "10") int topN,
            Model model) {
        
        // Lấy dữ liệu từ MongoDB
        List<RetailSummary> allData = forecastService.getMonthlyCategoryRevenue();
        
        // Lọc theo category nếu cần
        if (!selectedCategory.equals("ALL")) {
            allData = allData.stream()
                    .filter(d -> d.getCategory().equals(selectedCategory))
                    .collect(Collectors.toList());
        }
        
        // Tính KPIs
        double totalRevenue = allData.stream().mapToDouble(RetailSummary::getTotalRevenue).sum();
        Set<String> categories = allData.stream().map(RetailSummary::getCategory).collect(Collectors.toSet());
        Set<String> months = allData.stream().map(RetailSummary::getMonth).collect(Collectors.toSet());
        
        // Tìm top category
        Map<String, Double> categoryTotals = new HashMap<>();
        for (RetailSummary item : allData) {
            categoryTotals.merge(item.getCategory(), item.getTotalRevenue(), Double::sum);
        }
        String topCategory = categoryTotals.isEmpty() ? "N/A" : 
                Collections.max(categoryTotals.entrySet(), Map.Entry.comparingByValue()).getKey();
        
        // Dự đoán (đơn giản: average * forecastMonths)
        double avgMonthlyRevenue = months.isEmpty() ? 0 : totalRevenue / months.size();
        double forecastRevenue = avgMonthlyRevenue * forecastMonths;
        double growthRate = totalRevenue > 0 ? ((avgMonthlyRevenue - totalRevenue / months.size()) / (totalRevenue / months.size())) * 100 : 0;
        
        // Truyền data vào model
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("forecastRevenue", forecastRevenue);
        model.addAttribute("growthRate", growthRate);
        model.addAttribute("categoryCount", categories.size());
        model.addAttribute("monthCount", months.size());
        model.addAttribute("topCategory", topCategory);
        model.addAttribute("selectedForecastMonths", forecastMonths);
        model.addAttribute("dataList", allData.stream().limit(topN * 3).collect(Collectors.toList()));
        
        return "revenue_forecast";
    }
}

