package g2.g2_gp_project.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import g2.g2_gp_project.dto.ProductAnalysisRequest;
import g2.g2_gp_project.repository.OrderAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAnalysisService {
    private final OrderAnalysisRepository orderAnalysisRepository;

    public List<Map<String, Object>> analyzeProducts(ProductAnalysisRequest request) {
        try {
            log.info("Product analysis request received: fromDate={}, toDate={}, countries={}, topN={}", 
                    request.getFromDate(), request.getToDate(), 
                    request.getCountries() != null ? Arrays.toString(request.getCountries()) : "null", 
                    request.getTopN());
            
            // Parse dates
            java.time.LocalDate fromDate = java.time.LocalDate.parse(request.getFromDate());
            java.time.LocalDate toDate = java.time.LocalDate.parse(request.getToDate());
            log.info("Parsed dates: fromDate={}, toDate={}", fromDate, toDate);

            // Frontend now sends country names (common) directly, e.g., "France", "Syria"
            // Just trim and validate, then use directly for database query
            List<String> countries = null;
            if (request.getCountries() != null && request.getCountries().length > 0) {
                log.info("Received country names: {}", Arrays.toString(request.getCountries()));
                
                List<String> countryNames = Arrays.stream(request.getCountries())
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                
                if (!countryNames.isEmpty()) {
                    countries = countryNames;
                    log.info("Using country names for query: {}", countries);
                } else {
                    log.warn("No valid country names found. Returning empty results.");
                    return new ArrayList<>();
                }
            }
            log.info("Final countries filter: {} (will query {} countries)", countries, countries != null ? countries.size() : "all");

            Integer topN = request.getTopN() != null ? request.getTopN() : 10;
            log.info("Querying top {} products with parameters: fromDate={}, toDate={}, countries={}, topN={}", 
                    topN, fromDate, toDate, countries, topN);

            // Query top products by country using JPA repository (Postgres)
            List<Map<String, Object>> rawResults;
            try {
                rawResults = orderAnalysisRepository.findTopProductsByCountry(
                        fromDate, toDate, countries, topN
                );
            } catch (Exception e) {
                log.error("Database query failed: ", e);
                throw e;
            }
            
            log.info("Query returned {} results", rawResults.size());
            if (!rawResults.isEmpty()) {
                Map<String, Object> firstRow = rawResults.get(0);
                log.info("=== DEBUG: First result from query ===");
                log.info("Keys in result: {}", firstRow.keySet());
                log.info("Full row data: {}", firstRow);
                
                // Debug: try all possible country key variations
                log.info("Trying to find country field:");
                log.info("  - 'country': {}", firstRow.get("country"));
                log.info("  - 'COUNTRY': {}", firstRow.get("COUNTRY"));
                log.info("  - 'Country': {}", firstRow.get("Country"));
                for (String key : firstRow.keySet()) {
                    log.info("  - Key '{}': Value '{}' (Type: {})", key, firstRow.get(key), 
                            firstRow.get(key) != null ? firstRow.get(key).getClass().getName() : "null");
                }
                log.info("=== END DEBUG ===");
            } else {
                log.warn("No results returned. Check if data exists for date range {} to {}", fromDate, toDate);
            }

            // Process results: add rank, format fields
            // When multiple countries selected, each result shows its specific country
            List<Map<String, Object>> processedResults = new ArrayList<>();
            int rank = 1;
            for (Map<String, Object> row : rawResults) {
                Map<String, Object> item = new HashMap<>();
                item.put("rank", rank++);
                item.put("stockCode", row.get("stockCode"));
                item.put("description", row.get("description"));
                
                // Get country from query result (already grouped by country)
                // Try all possible key variations
                Object countryFromResult = null;
                String countryKey = null;
                
                // Try exact matches first
                if (row.containsKey("country")) {
                    countryFromResult = row.get("country");
                    countryKey = "country";
                } else if (row.containsKey("COUNTRY")) {
                    countryFromResult = row.get("COUNTRY");
                    countryKey = "COUNTRY";
                } else if (row.containsKey("Country")) {
                    countryFromResult = row.get("Country");
                    countryKey = "Country";
                } else {
                    // Try case-insensitive search
                    for (String key : row.keySet()) {
                        if (key != null && key.equalsIgnoreCase("country")) {
                            countryFromResult = row.get(key);
                            countryKey = key;
                            log.info("Found country using case-insensitive key: {} = {}", key, countryFromResult);
                            break;
                        }
                    }
                }
                
                // Always set country field (string), never countries (array)
                // Each row should have exactly one country since we GROUP BY country
                if (countryFromResult != null && countryFromResult.toString().trim().length() > 0) {
                    String countryStr = countryFromResult.toString().trim();
                    item.put("country", countryStr); // Always use "country" (singular), never "countries"
                    log.info("✓ Added country '{}' from key '{}' to result", countryStr, countryKey);
                } else {
                    // Country is required - this is a critical error
                    log.error("❌ CRITICAL: Country field not found in query result!");
                    log.error("Available keys: {}", row.keySet());
                    log.error("Full row data: {}", row);
                    log.error("Stock code: {}, Description: {}", row.get("stockCode"), row.get("description"));
                    log.error("This should not happen - query should include country in GROUP BY and SELECT");
                    
                    // Skip this row if country is missing (don't add incomplete data)
                    log.warn("Skipping row due to missing country field");
                    continue; // Skip to next row
                }
                
                item.put("totalQuantity", row.get("totalQuantity"));
                item.put("totalRevenue", row.get("totalRevenue"));
                
                // NEVER set "countries" array - each product should have exactly one country
                processedResults.add(item);
            }
            
            // Log final processed results to verify country is included
            if (!processedResults.isEmpty()) {
                log.info("Sample processed result (first): {}", processedResults.get(0));
            }
            
            log.info("Returning {} processed results", processedResults.size());
            return processedResults;
        } catch (Exception e) {
            log.error("Error analyzing products: ", e);
            throw new RuntimeException("Failed to analyze products: " + e.getMessage(), e);
        }
    }

    public List<String> getAllCountries() {
        // Get countries from PostgreSQL (customers table) instead of MongoDB
        return orderAnalysisRepository.findAllCountries()
            .stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .sorted()
            .collect(Collectors.toList());
    }
}