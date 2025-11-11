package com.example.mdm.service;

import com.example.mdm.events.CrEvents;
import com.example.mdm.model.ChangeRequest;
import com.example.mdm.repository.ChangeRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChangeRequestService {
    private final ChangeRequestRepository repo;
    private final ObjectMapper objectMapper;
    private final CrEvents events;

    public ChangeRequestService(ChangeRequestRepository repo, ObjectMapper objectMapper, CrEvents events) {
        this.repo = repo;
        this.objectMapper = objectMapper;
        this.events = events;
    }

    public UUID createDraft(Map<String, Object> body, boolean dryRun, String customerId) {
        UUID id = UUID.randomUUID();
        String status = dryRun ? "PENDING_APPROVAL" : "CREATED";

        ChangeRequest cr = new ChangeRequest();
        cr.setId(id);
        cr.setCustomerId(customerId);
        cr.setDomain((String) body.getOrDefault("domain", ""));
        cr.setTableName((String) body.getOrDefault("table", ""));
        cr.setOperation((String) body.getOrDefault("operation", ""));
        cr.setFilterJson(toJson(body.get("filter")));
        cr.setProposedChangesJson(toJson(body.get("proposed_changes")));
        // simple placeholder diff for now
        cr.setDiffPreviewJson(toJson(Map.of("rows", 1)));
        cr.setRiskScore(10);
        cr.setStatus(status);
        cr.setIdempotencyKey(null);
        repo.insert(cr);

        events.created(id.toString(), status);
        return id;
    }

    public Optional<ChangeRequest> get(UUID id) {
        return repo.findById(id);
    }

    public boolean approve(UUID id) {
        int n = repo.updateStatus(id, "APPROVED");
        if (n > 0) {
            events.approved(id.toString());
            return true;
        }
        return false;
    }

    public boolean apply(UUID id, String idempotencyKey) {
        int n = repo.updateApplied(id, idempotencyKey);
        if (n > 0) {
            events.applied(id.toString(), idempotencyKey);
            return true;
        }
        return false;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj == null ? Map.of() : obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }
}
