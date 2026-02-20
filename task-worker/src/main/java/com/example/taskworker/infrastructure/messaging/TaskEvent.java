package com.example.taskworker.infrastructure.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskEvent {
    private UUID taskId;
    private String taskType;
    private String payload;
}
