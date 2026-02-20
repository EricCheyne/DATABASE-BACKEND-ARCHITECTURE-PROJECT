package com.example.taskservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducer {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;

    @Value("${app.kafka.topics.task-submission:tasks.main}")
    private String topic;

    public void sendTaskEvent(TaskEvent event) {
        log.info("Publishing task event for task ID: {}", event.getTaskId());
        kafkaTemplate.send(topic, event.getTaskId().toString(), event)
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
