package g2.g2_gp_project.controller;

import g2.g2_gp_project.entity.RawTransaction;
import g2.g2_gp_project.service.MongoDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class MongoDataController {
    
    @Autowired
    private MongoDataService mongoDataService;

    /**
     * Hiển thị trang danh sách dữ liệu thô MongoDB (Hỗ trợ Search & Pagination)
     */
    @GetMapping("/mongo/list")
    public String showMongoDataList(
            Model model,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        // Sắp xếp theo _id giảm dần (MongoDB default)
        PageRequest pageable = PageRequest.of(page - 1, size);
        
        // Truyền keyword vào Service
        Page<RawTransaction> dataPage = mongoDataService.getMongoDataPaginated(keyword, pageable);

        model.addAttribute("dataPage", dataPage);
        model.addAttribute("keyword", keyword);

        // Logic tạo số trang (chỉ hiển thị 10 trang xung quanh current page)
        int totalPages = dataPage.getTotalPages();
        int currentPage = page;
        
        if (totalPages > 0) {
            int maxPagesToShow = 10;
            int startPage = Math.max(1, currentPage - 5);
            int endPage = Math.min(totalPages, currentPage + 4);
            
            // Đảm bảo luôn hiển thị đủ 10 trang nếu có thể
            if (endPage - startPage + 1 < maxPagesToShow) {
                if (startPage == 1) {
                    endPage = Math.min(totalPages, maxPagesToShow);
                } else if (endPage == totalPages) {
                    startPage = Math.max(1, totalPages - maxPagesToShow + 1);
                }
            }
            
            List<Integer> pageNumbers = IntStream.rangeClosed(startPage, endPage)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
            model.addAttribute("showFirstPage", startPage > 1);
            model.addAttribute("showLastPage", endPage < totalPages);
        }

        return "mongo_list";
    }
    
    /**
     * Hiển thị chi tiết sản phẩm
     */
    @GetMapping("/mongo/product/{stockCode}")
    public String showProductDetail(@PathVariable String stockCode, Model model) {
        try {
            List<RawTransaction> transactions = mongoDataService.getProductByStockCode(stockCode);
            
            if (transactions.isEmpty()) {
                model.addAttribute("error", "Không tìm thấy sản phẩm với Stock Code: " + stockCode);
                return "product_detail";
            }
            
            RawTransaction product = transactions.get(0);
            
            // Tính thống kê
            int totalQty = 0;
            double totalRev = 0;
            Set<String> countries = new HashSet<>();
            
            for (RawTransaction t : transactions) {
                try {
                    if (t.getQuantity() != null && !t.getQuantity().isEmpty()) {
                        int qty = Integer.parseInt(t.getQuantity());
                        double price = Double.parseDouble(t.getUnitPrice());
                        if (qty > 0 && price > 0) {
                            totalQty += qty;
                            totalRev += qty * price;
                        }
                    }
                    if (t.getCountry() != null && !t.getCountry().isEmpty()) {
                        countries.add(t.getCountry());
                    }
                } catch (Exception e) {
                    // Skip invalid data
                }
            }
            
            model.addAttribute("product", product);
            model.addAttribute("stockCode", stockCode);
            model.addAttribute("totalTransactions", transactions.size());
            model.addAttribute("totalQuantity", totalQty);
            model.addAttribute("totalRevenue", totalRev);
            model.addAttribute("countryCount", countries.size());
            model.addAttribute("recentTransactions", transactions.stream().limit(10).collect(Collectors.toList()));
            
            return "product_detail";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "product_detail";
        }
    }
}


