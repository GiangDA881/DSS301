package g2.g2_gp_project.repository;


import g2.g2_gp_project.entity.SalesSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalesSummaryRepository extends JpaRepository<SalesSummary, Long> {
    List<SalesSummary> findByCountry(String country);
    List<SalesSummary> findByCountryOrderByRankingAsc(String country);
    List<SalesSummary> findByAnalysisDate(LocalDate date);

    @Query("SELECT s FROM SalesSummary s WHERE s.country = :country AND s.ranking <= :topN ORDER BY s.ranking")
    List<SalesSummary> findTopNByCountry(@Param("country") String country, @Param("topN") int topN);

    @Query("SELECT DISTINCT s.country FROM SalesSummary s ORDER BY s.country")
    List<String> findAllCountriesInSummary();
}
