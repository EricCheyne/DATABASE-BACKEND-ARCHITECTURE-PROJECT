package com.example.taskservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducer {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;

    @Value("${app.kafka.topics.task-submission:tasks.main}")
    private String topic;

    public void sendTaskEvent(TaskEvent event) {
        log.info("Publishing task event for task ID: {}", event.getTaskId());
        
        String correlationId = MDC.get("correlationId");
        ProducerRecord<String, TaskEvent> record = new ProducerRecord<>(topic, event.getTaskId().toString(), event);
        
        if (correlationId != null) {
            record.headers().add(new RecordHeader("correlationId", correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published task ID: {} to partition: {}", 
                                event.getTaskId(), result.getRecordMetadata().partition());
                    } else {
                        log.error("Failed to publish task ID: {}", event.getTaskId(), ex);
                    }
                });
    }
}
