package com.example.mdm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import com.example.mdm.events.CrEvents;

@RestController
@RequestMapping("/cr")
@Validated
public class CrController {

    private final CrEvents events;

    public CrController(CrEvents events) {
        this.events = events;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCr(@RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
                                                        @RequestBody Map<String, Object> body) {
        // Stub: return a fake CR id and status
        String id = UUID.randomUUID().toString();
        String status = dryRun ? "PENDING_APPROVAL" : "CREATED";
        events.created(id, status);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", status,
                "diff_preview", Map.of("rows", 1)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCr(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "PENDING_APPROVAL"
        ));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String id, @RequestBody Map<String, Object> decision) {
        events.approved(id);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "APPROVED"
        ));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> apply(@PathVariable String id, @RequestHeader(name = "Idempotency-Key", required = false) String key) {
        events.applied(id, key);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "APPLIED",
                "idempotency_key", key
        ));
    }
}
