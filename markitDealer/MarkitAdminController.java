package fr.lbp.markit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/markit")
public class MarkitAdminController {

    private final LbpMarkitClient client;

    public MarkitAdminController(LbpMarkitClient client) {
        this.client = client;
    }

    @PostMapping("/restart")
    public ResponseEntity<String> restart() {
        client.restartConnector();
        return ResponseEntity.ok("OK - Markit connector restarted");
    }
}
