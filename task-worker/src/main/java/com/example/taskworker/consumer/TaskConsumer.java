package com.example.taskworker.consumer;

import com.example.taskworker.domain.model.Task;
import com.example.taskworker.domain.model.TaskStatus;
import com.example.taskworker.domain.repository.TaskRepository;
import com.example.taskworker.infrastructure.lock.RedisLockHelper;
import com.example.taskworker.infrastructure.messaging.TaskEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskConsumer {

    private final TaskRepository taskRepository;
    private final RedisLockHelper redisLockHelper;
    private final MeterRegistry meterRegistry;

    @Transactional
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "${app.kafka.task-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TaskEvent event) {
        log.info("Received task event: {}", event);
        meterRegistry.counter("tasks.received", "type", event.getTaskType()).increment();

        String lockKey = "lock:task:" + event.getTaskId();

        redisLockHelper.executeWithLock(lockKey, Duration.ofMinutes(5), () -> {
            Task task = taskRepository.findById(event.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found: " + event.getTaskId()));

            if (task.getStatus() == TaskStatus.SUCCESS) {
                log.info("Task {} already processed successfully. Skipping.", task.getId());
                return null;
            }

            try {
                processTask(task);
                task.setStatus(TaskStatus.SUCCESS);
                meterRegistry.counter("tasks.processed.success", "type", event.getTaskType()).increment();
            } catch (Exception e) {
                log.error("Error processing task {}: {}", task.getId(), e.getMessage());
                task.setStatus(TaskStatus.FAILED);
                task.setRetryCount(task.getRetryCount() + 1);
                meterRegistry.counter("tasks.processed.failure", "type", event.getTaskType()).increment();
                throw e; // Throw exception to trigger retry
            } finally {
                taskRepository.save(task);
            }
            return null;
        });
    }

    private void processTask(Task task) {
        log.info("Processing task: {}", task.getId());
        // Business logic here
        // Simulating processing
        if (task.getPayload().contains("fail")) {
            throw new RuntimeException("Simulated processing failure");
        }
    }
}
