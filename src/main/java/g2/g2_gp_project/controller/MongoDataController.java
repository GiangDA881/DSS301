package g2.g2_gp_project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import g2.g2_gp_project.mongo.MongoInvoice;
import g2.g2_gp_project.mongo.MongoInvoiceRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mongo")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MongoDataController {

    private final MongoInvoiceRepository mongoInvoiceRepository;

    @GetMapping("/invoices")
    public List<MongoInvoice> getAllInvoices() {
        return mongoInvoiceRepository.findAll();
    }
}