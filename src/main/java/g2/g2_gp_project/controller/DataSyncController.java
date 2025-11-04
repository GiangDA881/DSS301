package g2.g2_gp_project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import g2.g2_gp_project.service.MongoPostgresDataSync;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DataSyncController {

    private final MongoPostgresDataSync mongoPostgresDataSync;

    @PostMapping("/sync")
    public ResponseEntity<?> syncData() {
        try {
            mongoPostgresDataSync.syncData();
            return ResponseEntity.ok().body(java.util.Map.of(
                "status", "success",
                "message", "Data sync completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                "status", "error",
                "message", "Failed to sync data: " + e.getMessage()
            ));
        }
    }
}