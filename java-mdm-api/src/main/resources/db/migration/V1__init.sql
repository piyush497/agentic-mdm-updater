-- Schemas
CREATE SCHEMA IF NOT EXISTS ops;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS mdm;

-- Change Request
CREATE TABLE IF NOT EXISTS ops.change_request (
    id UUID PRIMARY KEY,
    customer_id TEXT,
    domain TEXT,
    table_name TEXT,
    operation TEXT,
    filter_json JSONB,
    proposed_changes_json JSONB,
    diff_preview_json JSONB,
    risk_score INT,
    status TEXT,
    idempotency_key TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Approval
CREATE TABLE IF NOT EXISTS ops.approval (
    id UUID PRIMARY KEY,
    change_request_id UUID REFERENCES ops.change_request(id),
    approver_id TEXT,
    decision TEXT,
    comment TEXT,
    policy_rule TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Audit
CREATE TABLE IF NOT EXISTS ops.audit_log (
    id UUID PRIMARY KEY,
    change_request_id UUID,
    event_type TEXT,
    actor TEXT,
    payload_json JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Outbox
CREATE TABLE IF NOT EXISTS ops.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type TEXT,
    aggregate_id TEXT,
    event_type TEXT,
    payload_json JSONB,
    status TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    sent_at TIMESTAMPTZ
);

-- Catalog docs
CREATE TABLE IF NOT EXISTS catalog.schema_docs (
    table_name TEXT,
    column_name TEXT,
    description TEXT,
    enum_values TEXT,
    examples TEXT
);

-- Seed MDM tables
CREATE TABLE IF NOT EXISTS mdm.supplier (
    supplier_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS mdm.supplier_address (
    address_id SERIAL PRIMARY KEY,
    supplier_id INT REFERENCES mdm.supplier(supplier_id),
    address_line1 TEXT,
    city TEXT,
    country TEXT
);

INSERT INTO mdm.supplier(name) VALUES ('Acme') ON CONFLICT DO NOTHING;
INSERT INTO mdm.supplier_address(supplier_id, address_line1, city, country)
SELECT 1, '1 Main St', 'Old City', 'US'
WHERE NOT EXISTS (SELECT 1 FROM mdm.supplier_address WHERE supplier_id=1);
