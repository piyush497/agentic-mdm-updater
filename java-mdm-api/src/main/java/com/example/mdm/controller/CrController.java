package com.example.mdm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import com.example.mdm.events.CrEvents;
import com.example.mdm.model.ChangeRequest;
import com.example.mdm.service.ChangeRequestService;

@RestController
@RequestMapping("/cr")
@Validated
public class CrController {

    private final CrEvents events;
    private final ChangeRequestService service;

    public CrController(CrEvents events, ChangeRequestService service) {
        this.events = events;
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCr(@RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
                                                        @RequestBody Map<String, Object> body) {
        UUID id = service.createDraft(body, dryRun, (String) body.getOrDefault("customer_id", "anonymous"));
        return service.get(id)
                .map(CrController::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.internalServerError().body(Map.of("error", "failed_to_create")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCr(@PathVariable String id) {
        return service.get(UUID.fromString(id))
                .map(CrController::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String id, @RequestBody Map<String, Object> decision) {
        boolean ok = service.approve(UUID.fromString(id));
        if (!ok) return ResponseEntity.notFound().build();
        return service.get(UUID.fromString(id))
                .map(CrController::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.internalServerError().body(Map.of("error", "approve_failed")));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> apply(@PathVariable String id, @RequestHeader(name = "Idempotency-Key", required = false) String key) {
        boolean ok = service.apply(UUID.fromString(id), key);
        if (!ok) return ResponseEntity.notFound().build();
        return service.get(UUID.fromString(id))
                .map(CrController::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.internalServerError().body(Map.of("error", "apply_failed")));
    }

    private static Map<String, Object> toResponse(ChangeRequest cr) {
        return Map.of(
                "id", cr.getId().toString(),
                "customer_id", cr.getCustomerId(),
                "domain", cr.getDomain(),
                "table", cr.getTableName(),
                "operation", cr.getOperation(),
                "filter", cr.getFilterJson(),
                "proposed_changes", cr.getProposedChangesJson(),
                "diff_preview", cr.getDiffPreviewJson(),
                "risk_score", cr.getRiskScore(),
                "status", cr.getStatus(),
                "idempotency_key", cr.getIdempotencyKey(),
                "created_at", String.valueOf(cr.getCreatedAt()),
                "updated_at", String.valueOf(cr.getUpdatedAt())
        );
    }
}
