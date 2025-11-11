-- Seed sample Change Requests for demo
INSERT INTO ops.change_request (
    id, customer_id, domain, table_name, operation, filter_json, proposed_changes_json, diff_preview_json, risk_score, status, idempotency_key, created_at, updated_at
) VALUES
    ('11111111-1111-1111-1111-111111111111', 'user_alice', 'supplier', 'supplier_address', 'UPDATE',
     '{"supplier_id": 1}'::jsonb,
     '{"city": "New City"}'::jsonb,
     '{"rows": 1, "preview": [{"before": {"city": "Old City"}, "after": {"city": "New City"}}]}'::jsonb,
     10, 'PENDING_APPROVAL', 'seed-key-1', now(), now()),
    ('22222222-2222-2222-2222-222222222222', 'user_bob', 'supplier', 'supplier_address', 'UPDATE',
     '{"supplier_id": 1}'::jsonb,
     '{"country": "US"}'::jsonb,
     '{"rows": 1, "preview": [{"before": {"country": "US"}, "after": {"country": "US"}}]}'::jsonb,
     5, 'APPROVED', 'seed-key-2', now() - interval '1 day', now() - interval '1 day'),
    ('33333333-3333-3333-3333-333333333333', 'user_carla', 'supplier', 'supplier_address', 'UPDATE',
     '{"supplier_id": 1}'::jsonb,
     '{"address_line1": "100 Market St"}'::jsonb,
     '{"rows": 1, "preview": [{"before": {"address_line1": "1 Main St"}, "after": {"address_line1": "100 Market St"}}]}'::jsonb,
     20, 'APPLIED', 'seed-key-3', now() - interval '2 days', now() - interval '2 days');
