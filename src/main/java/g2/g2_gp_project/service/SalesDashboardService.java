package g2.g2_gp_project.service;


import g2.g2_gp_project.repository.SalesSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesDashboardService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SalesSummaryRepository salesSummaryRepository;
        /**
         * Lấy doanh số theo quốc gia với filter theo thời gian
         */
        public List<Map<String, Object>> getSalesByCountry(LocalDateTime startDate, LocalDateTime endDate) {
            StringBuilder sql = new StringBuilder("""
            SELECT 
                o.order_date,
                c.country,
                c.region,
                COUNT(DISTINCT o.order_id) as total_orders,
                COUNT(DISTINCT o.customer_id) as total_customers,
                SUM(oi.quantity) as total_quantity,
                SUM(oi.quantity * oi.unit_price) as total_revenue,
                AVG(oi.quantity * oi.unit_price) as avg_order_value
            FROM orders o
            JOIN customers c ON o.customer_id = c.customer_id
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE 1=1
            """);

            List<Object> params = new ArrayList<>();

            if (startDate != null) {
                sql.append(" AND o.created_at >= ? ");
                params.add(startDate);
            }

            if (endDate != null) {
                sql.append(" AND o.created_at <= ? ");
                params.add(endDate);
            }

            sql.append("""
            GROUP BY o.order_date, c.country, c.region
            ORDER BY total_revenue DESC
            """);

            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        }

        /**
         * Lấy top sản phẩm bán chạy theo quốc gia
         */
        public List<Map<String, Object>> getTopProductsByCountry(String country, int limit) {
            String sql = """
            SELECT 
                p.stock_code,
                p.description,
                p.category,
                SUM(oi.quantity) as total_sold,
                SUM(oi.quantity * oi.unit_price) as revenue,
                COUNT(DISTINCT o.order_id) as order_count
            FROM products p
            JOIN order_items oi ON p.stock_code = oi.product_id
            JOIN orders o ON oi.order_id = o.order_id
            JOIN customers c ON o.customer_id = c.customer_id
            WHERE c.country = ?
            GROUP BY p.stock_code, p.description, p.category
            ORDER BY revenue DESC
            LIMIT ?
            """;

            return jdbcTemplate.queryForList(sql, country, limit);
        }

        /**
         * Lấy xu hướng doanh số theo tháng
         */
        public List<Map<String, Object>> getSalesTrend(String country, int months) {
            StringBuilder sql = new StringBuilder("""
            SELECT 
                TO_CHAR(o.order_date, 'YYYY-MM') as month,
                COUNT(DISTINCT o.order_id) as orders,
                SUM(oi.quantity) as quantity,
                SUM(oi.quantity * oi.unit_price) as revenue,
                COUNT(DISTINCT o.customer_id) as unique_customers
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            """);

            List<Object> params = new ArrayList<>();

            if (country != null) {
                sql.append("""
                JOIN customers c ON o.customer_id = c.customer_id
                WHERE c.country = ?
                AND o.order_date >= CURRENT_DATE - INTERVAL '""").append(months).append(" months '\n");
                params.add(country);
            } else {
                sql.append("WHERE o.order_date >= CURRENT_DATE - INTERVAL '").append(months).append(" months'\n");
            }

            sql.append("""
            GROUP BY TO_CHAR(o.order_date, 'YYYY-MM')
            ORDER BY month
            """);

            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        }

        /**
         * So sánh hiệu suất giữa các quốc gia
         */
        public List<Map<String, Object>> getCountryPerformanceComparison() {
            String sql = """
            WITH country_stats AS (
                SELECT 
                    c.country,
                    c.region,
                    COUNT(DISTINCT o.order_id) as total_orders,
                    COUNT(DISTINCT o.customer_id) as total_customers,
                    SUM(oi.quantity * oi.unit_price) as total_revenue,
                    AVG(oi.quantity * oi.unit_price) as avg_order_value
                FROM customers c
                JOIN orders o ON c.customer_id = o.customer_id
                JOIN order_items oi ON o.order_id = oi.order_id
                GROUP BY c.country, c.region
            ),
            rankings AS (
                SELECT 
                    *,
                    RANK() OVER (ORDER BY total_revenue DESC) as revenue_rank,
                    RANK() OVER (ORDER BY total_orders DESC) as orders_rank,
                    RANK() OVER (ORDER BY avg_order_value DESC) as aov_rank
                FROM country_stats
            )
            SELECT 
                country,
                region,
                total_orders,
                total_customers,
                total_revenue,
                avg_order_value,
                revenue_rank,
                orders_rank,
                aov_rank,
                ROUND((total_revenue / (SELECT SUM(total_revenue) FROM country_stats) * 100), 2) as revenue_share_pct
            FROM rankings
            ORDER BY total_revenue DESC
            """;

            return jdbcTemplate.queryForList(sql);
        }

        /**
         * Lấy tổng quan dashboard
         */
        public Map<String, Object> getDashboardOverview() {
            Map<String, Object> overview = new HashMap<>();

            try {
                // Tổng doanh thu
                String revenueSql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders";
                BigDecimal totalRevenue = jdbcTemplate.queryForObject(revenueSql, BigDecimal.class);
                overview.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

                // Tổng đơn hàng
                String ordersSql = "SELECT COUNT(*) FROM orders";
                Integer totalOrders = jdbcTemplate.queryForObject(ordersSql, Integer.class);
                overview.put("totalOrders", totalOrders != null ? totalOrders : 0);

                // Tổng khách hàng
                String customersSql = "SELECT COUNT(DISTINCT customer_id) FROM orders";
                Integer totalCustomers = jdbcTemplate.queryForObject(customersSql, Integer.class);
                overview.put("totalCustomers", totalCustomers != null ? totalCustomers : 0);

                // Số quốc gia
                String countriesSql = "SELECT COUNT(DISTINCT country) FROM customers";
                Integer totalCountries = jdbcTemplate.queryForObject(countriesSql, Integer.class);
                overview.put("totalCountries", totalCountries != null ? totalCountries : 0);

                // Giá trị đơn hàng trung bình
                if (totalOrders != null && totalOrders > 0 && totalRevenue != null) {
                    overview.put("avgOrderValue",
                            totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP));
                } else {
                    overview.put("avgOrderValue", BigDecimal.ZERO);
                }

                // Top 5 quốc gia
                String topCountriesSql = """
                SELECT c.country, COALESCE(SUM(o.total_amount), 0) as revenue
                FROM customers c
                JOIN orders o ON c.customer_id = o.customer_id
                GROUP BY c.country
                ORDER BY revenue DESC
                LIMIT 5
                """;
                overview.put("topCountries", jdbcTemplate.queryForList(topCountriesSql));

                // Xu hướng 7 ngày gần nhất
                String trendSql = """
                SELECT 
                    o.order_date,
                    COUNT(*) as orders,
                    COALESCE(SUM(o.total_amount), 0) as revenue
                FROM orders o
                WHERE o.order_date >= CURRENT_DATE - INTERVAL '7 days'
                GROUP BY o.order_date
                ORDER BY o.order_date
                """;
                overview.put("recentTrend", jdbcTemplate.queryForList(trendSql));

            } catch (Exception e) {
                e.printStackTrace();
                // Return empty values on error
                overview.put("totalRevenue", BigDecimal.ZERO);
                overview.put("totalOrders", 0);
                overview.put("totalCustomers", 0);
                overview.put("totalCountries", 0);
                overview.put("avgOrderValue", BigDecimal.ZERO);
                overview.put("topCountries", new ArrayList<>());
                overview.put("recentTrend", new ArrayList<>());
                overview.put("error", e.getMessage());
            }

            return overview;
        }

        /**
         * Phân tích theo danh mục sản phẩm
         */
        public List<Map<String, Object>> getCategoryAnalysis(String country) {
            StringBuilder sql = new StringBuilder("""
            SELECT 
                p.category,
                COUNT(DISTINCT p.stock_code) as product_count,
                SUM(oi.quantity) as total_quantity,
                SUM(oi.quantity * oi.unit_price) as total_revenue,
                AVG(oi.quantity * oi.unit_price) as avg_sale
            FROM products p
            JOIN order_items oi ON p.stock_code = oi.product_id
            JOIN orders o ON oi.order_id = o.order_id
            """);

            List<Object> params = new ArrayList<>();

            if (country != null) {
                sql.append("""
                JOIN customers c ON o.customer_id = c.customer_id
                WHERE c.country = ?
                """);
                params.add(country);
            }

            sql.append("""
            GROUP BY p.category
            ORDER BY total_revenue DESC
            """);

            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        }

        /**
         * Dữ liệu cho heatmap (quốc gia x tháng)
         */
        public List<Map<String, Object>> getSalesHeatmap() {
            String sql = """
            SELECT 
                c.country,
                TO_CHAR(o.order_date, 'YYYY-MM') as month,
                SUM(oi.quantity * oi.unit_price) as revenue,
                COUNT(DISTINCT o.order_id) as orders
            FROM customers c
            JOIN orders o ON c.customer_id = o.customer_id
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE o.order_date >= CURRENT_DATE - INTERVAL '12 months'
            GROUP BY c.country, TO_CHAR(o.order_date, 'YYYY-MM')
            ORDER BY month, country
            """;

            return jdbcTemplate.queryForList(sql);
        }
    /**
     * Dữ liệu cho heatmap (quốc gia x tháng)
     */

    /**
     * Lấy thống kê chi tiết cho một quốc gia cụ thể
     */
    public Map<String, Object> getCountryDetailStats(String country, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                c.country,
                COUNT(DISTINCT o.order_id) as total_orders,
                COUNT(DISTINCT o.customer_id) as total_customers,
                SUM(oi.quantity * oi.unit_price) as total_revenue,
                AVG(oi.quantity * oi.unit_price) as avg_order_value
            FROM customers c
            JOIN orders o ON c.customer_id = o.customer_id
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE c.country = ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(country);

        if (startDate != null) {
            sql.append(" AND o.created_at >= ? ");
            params.add(startDate);
        }

        if (endDate != null) {
            sql.append(" AND o.created_at <= ? ");
            params.add(endDate);
        }

        sql.append(" GROUP BY c.country");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        if (results.isEmpty()) {
            // Return empty stats if no data found
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("country", country);
            emptyStats.put("total_orders", 0);
            emptyStats.put("total_customers", 0);
            emptyStats.put("total_revenue", BigDecimal.ZERO);
            emptyStats.put("avg_order_value", BigDecimal.ZERO);
            return emptyStats;
        }

        return results.get(0);
    }
}

