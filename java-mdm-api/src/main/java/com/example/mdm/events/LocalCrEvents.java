package com.example.mdm.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("local")
public class LocalCrEvents implements CrEvents {
    private static final Logger log = LoggerFactory.getLogger(LocalCrEvents.class);
    private final ApplicationEventPublisher publisher;

    public LocalCrEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void created(String id, String status) {
        var evt = Map.of("type", "CR_CREATED", "id", id, "status", status);
        log.info("[LOCAL] {}", evt);
        publisher.publishEvent(evt);
    }

    @Override
    public void approved(String id) {
        var evt = Map.of("type", "CR_APPROVED", "id", id);
        log.info("[LOCAL] {}", evt);
        publisher.publishEvent(evt);
    }

    @Override
    public void applied(String id, String idempotencyKey) {
        var evt = Map.of("type", "CR_APPLIED", "id", id, "idempotency_key", idempotencyKey);
        log.info("[LOCAL] {}", evt);
        publisher.publishEvent(evt);
    }
}
