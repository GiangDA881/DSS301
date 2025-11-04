package g2.g2_gp_project.service;


import g2.g2_gp_project.entity.*;
import g2.g2_gp_project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalesETLService {
    @Autowired
    private RetailDataRepository retailDataRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private SalesSummaryRepository salesSummaryRepository;

    @Transactional
    public void executeETL() {
        System.out.println("🔄 Bắt đầu ETL từ MongoDB sang PostgreSQL...");

        // Lấy dữ liệu từ MongoDB
        List<RetailData> rawData = retailDataRepository.findAll();

        if (rawData.isEmpty()) {
            System.out.println("⚠️ Không có dữ liệu trong MongoDB");
            return;
        }

        // 1. Extract & Transform Customers
        extractCustomers(rawData);

        // 2. Extract & Transform Products
        extractProducts(rawData);

        // 3. Extract & Transform Orders + OrderItems
        extractOrders(rawData);

        // 4. Generate Sales Summary (OLAP)
        generateSalesSummary(rawData);

        System.out.println("✅ ETL hoàn tất!");
    }

    /**
     * Trích xuất và lưu Customers
     */
    private void extractCustomers(List<RetailData> rawData) {
        Map<String, Customer> customerMap = new HashMap<>();

        for (RetailData data : rawData) {
            String customerId = data.getCustomerID();
            if (customerId == null || customerId.trim().isEmpty()) continue;

            if (!customerMap.containsKey(customerId)) {
                Customer customer = new Customer();
                customer.setCustomerId(customerId);
                customer.setCountry(data.getCountry());
                // Các trường khác có thể để null hoặc generate
                customer.setCustomerName("Customer " + customerId);
                customer.setRegion(determineRegion(data.getCountry()));

                customerMap.put(customerId, customer);
            }
        }

        // Lưu vào PostgreSQL
        customerRepository.saveAll(customerMap.values());
        System.out.println("📊 Đã lưu " + customerMap.size() + " customers");
    }

    /**
     * Trích xuất và lưu Products
     */
    private void extractProducts(List<RetailData> rawData) {
        Map<String, Product> productMap = new HashMap<>();

        for (RetailData data : rawData) {
            String stockCode = data.getStockCode();
            if (stockCode == null || stockCode.trim().isEmpty()) continue;

            if (!productMap.containsKey(stockCode)) {
                Product product = new Product();
                product.setStockCode(stockCode);
                product.setDescription(data.getDescription());
                product.setUnitPrice(BigDecimal.valueOf(data.getUnitPrice()));
                product.setCategory(categorizeProduct(data.getDescription()));

                productMap.put(stockCode, product);
            }
        }

        productRepository.saveAll(productMap.values());
        System.out.println("📦 Đã lưu " + productMap.size() + " products");
    }

    /**
     * Trích xuất và lưu Orders + Order Items
     */
    private void extractOrders(List<RetailData> rawData) {
        // Group by InvoiceNo
        Map<String, List<RetailData>> orderGroups = rawData.stream()
                .filter(d -> d.getInvoiceNo() != null && d.getCustomerID() != null)
                .collect(Collectors.groupingBy(RetailData::getInvoiceNo));

        for (Map.Entry<String, List<RetailData>> entry : orderGroups.entrySet()) {
            String invoiceNo = entry.getKey();
            List<RetailData> items = entry.getValue();

            if (items.isEmpty()) continue;

            // Tạo Order
            Order order = new Order();
            order.setOrderId(invoiceNo);
            order.setCustomer(items.get(0).getCustomerID());
            order.setOrderDate(items.get(0).getInvoiceDate().toLocalDate());
            order.setStatus("Completed");

            // Tính tổng tiền
            BigDecimal subtotal = BigDecimal.ZERO;
            for (RetailData item : items) {
                BigDecimal itemTotal = BigDecimal.valueOf(item.getQuantity() * item.getUnitPrice());
                subtotal = subtotal.add(itemTotal);
            }

            order.setSubtotal(subtotal);
            order.setTax(subtotal.multiply(BigDecimal.valueOf(0.1))); // 10% tax
            order.setShippingFee(BigDecimal.valueOf(5.00));
            order.setTotalAmount(order.getSubtotal().add(order.getTax()).add(order.getShippingFee()));
            order.setPaymentMethod("Card");

            // Lưu Order
            orderRepository.save(order);

            // Tạo Order Items
            for (RetailData item : items) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(invoiceNo);
                orderItem.setProduct(item.getStockCode());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(BigDecimal.valueOf(item.getUnitPrice()));

                orderItemRepository.save(orderItem);
            }
        }

        System.out.println("🛒 Đã lưu " + orderGroups.size() + " orders");
    }

    /**
     * Tạo Sales Summary theo quốc gia (OLAP)
     */
    private void generateSalesSummary(List<RetailData> rawData) {
        // Group by Country and StockCode
        Map<String, Map<String, List<RetailData>>> countryProductMap = rawData.stream()
                .filter(d -> d.getCountry() != null && d.getStockCode() != null)
                .collect(Collectors.groupingBy(
                        RetailData::getCountry,
                        Collectors.groupingBy(RetailData::getStockCode)
                ));

        List<SalesSummary> summaries = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<RetailData>>> countryEntry : countryProductMap.entrySet()) {
            String country = countryEntry.getKey();

            for (Map.Entry<String, List<RetailData>> productEntry : countryEntry.getValue().entrySet()) {
                String stockCode = productEntry.getKey();
                List<RetailData> sales = productEntry.getValue();

                int totalQuantity = sales.stream().mapToInt(RetailData::getQuantity).sum();
                double totalRevenue = sales.stream()
                        .mapToDouble(s -> s.getQuantity() * s.getUnitPrice())
                        .sum();

                SalesSummary summary = new SalesSummary();
                summary.setCountry(country);
                summary.setStockCode(stockCode);
                summary.setTotalQuantity(totalQuantity);
                summary.setTotalRevenue(BigDecimal.valueOf(totalRevenue).setScale(2, RoundingMode.HALF_UP));
                summary.setAnalysisDate(LocalDate.now());

                summaries.add(summary);
            }
        }

        // Tính ranking theo revenue cho mỗi quốc gia
        Map<String, List<SalesSummary>> summariesByCountry = summaries.stream()
                .collect(Collectors.groupingBy(SalesSummary::getCountry));

        for (List<SalesSummary> countrySummaries : summariesByCountry.values()) {
            countrySummaries.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
            for (int i = 0; i < countrySummaries.size(); i++) {
                countrySummaries.get(i).setRanking(i + 1);
            }
        }

        salesSummaryRepository.saveAll(summaries);
        System.out.println("📈 Đã tạo " + summaries.size() + " sales summaries");
    }

    // Helper methods
    private String determineRegion(String country) {
        if (country == null) return "Unknown";
        if (country.contains("United Kingdom")) return "Europe";
        if (country.contains("Germany") || country.contains("France")) return "Europe";
        if (country.contains("Australia")) return "Oceania";
        if (country.contains("Japan") || country.contains("Singapore")) return "Asia";
        return "Other";
    }

    private String categorizeProduct(String description) {
        if (description == null) return "Uncategorized";
        description = description.toLowerCase();
        if (description.contains("bag") || description.contains("box")) return "Packaging";
        if (description.contains("light") || description.contains("lamp")) return "Lighting";
        if (description.contains("heart") || description.contains("love")) return "Gifts";
        if (description.contains("christmas") || description.contains("xmas")) return "Seasonal";
        return "General";
    }


}
