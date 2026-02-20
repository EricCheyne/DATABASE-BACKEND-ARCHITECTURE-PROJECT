package com.example.taskservice.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "Payload is required")
    private String payload;
}
