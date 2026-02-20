package com.example.taskservice.domain.service;

import com.example.taskservice.domain.model.Task;
import com.example.taskservice.domain.model.TaskStatus;
import com.example.taskservice.domain.repository.TaskRepository;
import com.example.taskservice.infrastructure.messaging.TaskEvent;
import com.example.taskservice.infrastructure.messaging.TaskProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskProducer taskProducer;

    @Transactional
    public Task createTask(String payload) {
        log.info("Creating new task");
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .payload(payload)
                .retryCount(0)
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created with ID: {}", savedTask.getId());

        // Publish to Kafka (Outbox pattern is better for strict consistency, but for this exercise we call it here)
        taskProducer.sendTaskEvent(new TaskEvent(savedTask.getId(), "DEFAULT", payload));

        return savedTask;
    }

    @Transactional(readOnly = true)
    public Task getTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + id));
    }
}
