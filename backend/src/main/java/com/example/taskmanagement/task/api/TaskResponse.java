package com.example.taskmanagement.task.api;

import com.example.taskmanagement.task.domain.Task;
import com.example.taskmanagement.task.domain.TaskPriority;
import com.example.taskmanagement.task.domain.TaskStatus;
import java.time.Instant;

public record TaskResponse(
    String id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    String assignee,
    Instant createdAt,
    Instant updatedAt) {
  public static TaskResponse from(Task task) {
    return new TaskResponse(
        task.id(),
        task.title(),
        task.description(),
        task.status(),
        task.priority(),
        task.assignee(),
        task.createdAt(),
        task.updatedAt());
  }
}
