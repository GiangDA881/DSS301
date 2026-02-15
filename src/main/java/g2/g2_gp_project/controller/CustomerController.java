package g2.g2_gp_project.controller;

import g2.g2_gp_project.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/list")
    public ResponseEntity<?> getCustomerList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(customerService.getCustomerList(page, size));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<?> getCustomerTransactions(@PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerTransactions(customerId));
    }

}
