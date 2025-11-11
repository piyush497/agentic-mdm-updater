package com.example.mdm.repository;

import com.example.mdm.model.ChangeRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ChangeRequestRepository {
    private final JdbcTemplate jdbc;

    public ChangeRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ChangeRequest> MAPPER = new RowMapper<>() {
        @Override
        public ChangeRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            ChangeRequest cr = new ChangeRequest();
            cr.setId((UUID) rs.getObject("id"));
            cr.setCustomerId(rs.getString("customer_id"));
            cr.setDomain(rs.getString("domain"));
            cr.setTableName(rs.getString("table_name"));
            cr.setOperation(rs.getString("operation"));
            cr.setFilterJson(rs.getString("filter_json"));
            cr.setProposedChangesJson(rs.getString("proposed_changes_json"));
            cr.setDiffPreviewJson(rs.getString("diff_preview_json"));
            int risk = rs.getInt("risk_score");
            cr.setRiskScore(rs.wasNull() ? null : risk);
            cr.setStatus(rs.getString("status"));
            cr.setIdempotencyKey(rs.getString("idempotency_key"));
            OffsetDateTime created = rs.getObject("created_at", OffsetDateTime.class);
            OffsetDateTime updated = rs.getObject("updated_at", OffsetDateTime.class);
            cr.setCreatedAt(created);
            cr.setUpdatedAt(updated);
            return cr;
        }
    };

    public void insert(ChangeRequest cr) {
        jdbc.update(
            "INSERT INTO ops.change_request (id, customer_id, domain, table_name, operation, filter_json, proposed_changes_json, diff_preview_json, risk_score, status, idempotency_key, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, CAST(? AS JSONB), CAST(? AS JSONB), CAST(? AS JSONB), ?, ?, ?, now(), now())",
            cr.getId(), cr.getCustomerId(), cr.getDomain(), cr.getTableName(), cr.getOperation(),
            cr.getFilterJson(), cr.getProposedChangesJson(), cr.getDiffPreviewJson(), cr.getRiskScore(), cr.getStatus(), cr.getIdempotencyKey()
        );
    }

    public Optional<ChangeRequest> findById(UUID id) {
        return jdbc.query("SELECT * FROM ops.change_request WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public int updateStatus(UUID id, String status) {
        return jdbc.update("UPDATE ops.change_request SET status = ?, updated_at = now() WHERE id = ?", status, id);
    }

    public int updateApplied(UUID id, String idempotencyKey) {
        return jdbc.update("UPDATE ops.change_request SET status = 'APPLIED', idempotency_key = ?, updated_at = now() WHERE id = ?", idempotencyKey, id);
    }
}
