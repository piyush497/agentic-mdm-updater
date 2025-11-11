package com.example.mdm.events;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalEventLogger {
    private static final Logger log = LoggerFactory.getLogger(LocalEventLogger.class);

    @EventListener
    public void onEvent(Map<String, Object> event) {
        // Simple local-only logger for CR lifecycle events
        log.info("[LOCAL EVENT] {}", event);
    }
}
