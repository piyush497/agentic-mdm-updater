package com.example.mdm.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ChangeRequest {
    private UUID id;
    private String customerId;
    private String domain;
    private String tableName;
    private String operation;
    private String filterJson; // JSONB
    private String proposedChangesJson; // JSONB
    private String diffPreviewJson; // JSONB
    private Integer riskScore;
    private String status;
    private String idempotencyKey;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getFilterJson() { return filterJson; }
    public void setFilterJson(String filterJson) { this.filterJson = filterJson; }
    public String getProposedChangesJson() { return proposedChangesJson; }
    public void setProposedChangesJson(String proposedChangesJson) { this.proposedChangesJson = proposedChangesJson; }
    public String getDiffPreviewJson() { return diffPreviewJson; }
    public void setDiffPreviewJson(String diffPreviewJson) { this.diffPreviewJson = diffPreviewJson; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
