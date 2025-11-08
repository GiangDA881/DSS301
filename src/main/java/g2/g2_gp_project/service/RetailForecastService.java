package g2.g2_gp_project.service;

import g2.g2_gp_project.dto.RetailSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RetailForecastService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<RetailSummary> getMonthlyCategoryRevenue() {
        try {
            System.out.println("🔍 Bắt đầu query MongoDB...");
            
            // Đơn giản hóa: Lấy ALL data từ MongoDB (không filter)
            List<org.bson.Document> rawResults = 
                    mongoTemplate.findAll(org.bson.Document.class, "retail_db");
            
            System.out.println("✅ Đã lấy " + rawResults.size() + " documents từ MongoDB");
            
            // Xử lý trong Java - Group theo Category + Month + Country
            Map<String, List<ItemData>> groupedData = new HashMap<>();
            int processedCount = 0;
            int errorCount = 0;
            
            for (org.bson.Document doc : rawResults) {
                try {
                    Object qtyObj = doc.get("Quantity");
                    Object priceObj = doc.get("UnitPrice");
                    Object invoiceDateObj = doc.get("InvoiceDate");
                    Object descriptionObj = doc.get("Description");
                    Object countryObj = doc.get("Country");
                    
                    if (qtyObj == null || priceObj == null || invoiceDateObj == null) {
                        errorCount++;
                        continue;
                    }
                    
                    String qtyStr = qtyObj.toString();
                    String priceStr = priceObj.toString();
                    String invoiceDate = invoiceDateObj.toString();
                    String description = descriptionObj != null ? descriptionObj.toString() : "UNKNOWN";
                    String country = countryObj != null ? countryObj.toString() : "Unknown";
                    
                    if (qtyStr.startsWith("-")) continue; // Skip returns
                    
                    double qty = Double.parseDouble(qtyStr);
                    double price = Double.parseDouble(priceStr);
                    
                    if (qty <= 0 || price <= 0) continue;
                    
                    double revenue = qty * price;
                    String month = invoiceDate.substring(0, 7); // YYYY-MM
                    String category = assignCategory(description);
                    
                    String key = category + "|" + month + "|" + country;
                    groupedData.computeIfAbsent(key, k -> new ArrayList<>())
                               .add(new ItemData(category, month, country, revenue));
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                }
            }
            
            System.out.println("✅ Đã xử lý: " + processedCount + " records, Lỗi: " + errorCount);
            
            // Convert to List<RetailSummary>
            List<RetailSummary> results = new ArrayList<>();
            for (Map.Entry<String, List<ItemData>> entry : groupedData.entrySet()) {
                List<ItemData> items = entry.getValue();
                if (items.isEmpty()) continue;
                
                ItemData first = items.get(0);
                double totalRevenue = items.stream().mapToDouble(i -> i.revenue).sum();
                
                results.add(new RetailSummary(first.category, first.month, first.country, totalRevenue));
            }
            
            // Sắp xếp theo month
            results.sort(Comparator.comparing(RetailSummary::getMonth));
            
            System.out.println("📊 Trả về " + results.size() + " kết quả");
            if (results.size() > 0) {
                System.out.println("📝 Ví dụ: " + results.get(0).getCategory() + " - " + results.get(0).getMonth() + " - $" + results.get(0).getTotalRevenue());
            }
            
            return results;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private String assignCategory(String description) {
        if (description == null) return "OTHER";
        String desc = description.toUpperCase();
        
        if (desc.contains("MUG") || desc.contains("CUP")) return "Kitchen";
        if (desc.contains("BAG") || desc.contains("TOTE")) return "Bags";
        if (desc.contains("CHRISTMAS") || desc.contains("SANTA")) return "Seasonal";
        if (desc.contains("CANDLE") || desc.contains("LIGHT")) return "Home Decor";
        if (desc.contains("HEART") || desc.contains("GIFT")) return "Gifts";
        if (desc.contains("PARTY")) return "Party";
        
        return "OTHER";
    }
    
    // Helper class
    private static class ItemData {
        String category;
        String month;
        String country;
        double revenue;
        
        ItemData(String category, String month, String country, double revenue) {
            this.category = category;
            this.month = month;
            this.country = country;
            this.revenue = revenue;
        }
    }
}

