package com.example.mdm.events;

public interface CrEvents {
    void created(String id, String status);
    void approved(String id);
    void applied(String id, String idempotencyKey);
}
