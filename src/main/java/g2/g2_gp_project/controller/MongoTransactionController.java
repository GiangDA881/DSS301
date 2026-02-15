package g2.g2_gp_project.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import g2.g2_gp_project.mongo.MongoTransaction;
import g2.g2_gp_project.mongo.MongoTransactionRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mongo")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MongoTransactionController {

    private final MongoTransactionRepository mongoTransactionRepository;

    @GetMapping("/transactions")
    public Map<String, Object> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit) {
        // Spring Data MongoDB paging
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, limit);
        org.springframework.data.domain.Page<MongoTransaction> paged = mongoTransactionRepository.findAll(pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("data", paged.getContent());
        result.put("total", paged.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }
}