package com.example.taskservice.domain.repository;

import com.example.taskservice.domain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, Task.TaskId> {
    Optional<Task> findById(UUID id);
}
