package com.example.mdm.events;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Profile("prod")
public class CloudCrEvents implements CrEvents {
    private static final Logger log = LoggerFactory.getLogger(CloudCrEvents.class);

    private final ServiceBusSenderClient sender;
    private final ObjectMapper objectMapper;

    public CloudCrEvents(
            @Value("${azure.servicebus.connection-string}") String connectionString,
            @Value("${azure.servicebus.topic}") String topic,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.sender = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(topic)
                .buildClient();
    }

    @Override
    public void created(String id, String status) {
        Map<String, Object> evt = new HashMap<>();
        evt.put("type", "CR_CREATED");
        evt.put("id", id);
        evt.put("status", status);
        send(evt);
    }

    @Override
    public void approved(String id) {
        Map<String, Object> evt = new HashMap<>();
        evt.put("type", "CR_APPROVED");
        evt.put("id", id);
        send(evt);
    }

    @Override
    public void applied(String id, String idempotencyKey) {
        Map<String, Object> evt = new HashMap<>();
        evt.put("type", "CR_APPLIED");
        evt.put("id", id);
        evt.put("idempotency_key", idempotencyKey);
        send(evt);
    }

    private void send(Map<String, Object> evt) {
        try {
            String payload = objectMapper.writeValueAsString(evt);
            sender.sendMessage(new ServiceBusMessage(payload));
            log.info("[PROD] sent event to Service Bus topic: {}", evt.get("type"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
