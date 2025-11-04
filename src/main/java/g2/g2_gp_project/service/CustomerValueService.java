package g2.g2_gp_project.service;

import g2.g2_gp_project.entity.RetailTransaction;
import g2.g2_gp_project.repository.RetailTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerValueService {

    @Autowired
    private RetailTransactionRepository repository;

    public List<Map<String, Object>> analyzeCustomerValue(String country, double threshold, int limit) {
        List<RetailTransaction> transactions;

        // Nếu country = "All" thì lấy toàn bộ, còn lại lọc theo country
        if (country.equalsIgnoreCase("All")) {
            transactions = repository.findAll();
        } else {
            transactions = repository.findByCountry(country);
        }

        // Nhóm theo CustomerID và tính tổng chi tiêu
        Map<String, Double> totalSpentByCustomer = transactions.stream()
                .filter(t -> t.getCustomerID() != null)
                .collect(Collectors.groupingBy(
                        RetailTransaction::getCustomerID,
                        Collectors.summingDouble(t -> t.getUnitPrice() * t.getQuantity())
                ));

        // Lọc theo ngưỡng threshold, sắp xếp giảm dần và giới hạn số lượng
        return totalSpentByCustomer.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // sort giảm dần theo totalSpent
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("customerID", entry.getKey());
                    map.put("totalSpent", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
