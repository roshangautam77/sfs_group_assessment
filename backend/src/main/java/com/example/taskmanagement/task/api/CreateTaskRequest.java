package com.example.taskmanagement.task.api;

import com.example.taskmanagement.task.domain.TaskChanges;
import com.example.taskmanagement.task.domain.TaskPriority;
import com.example.taskmanagement.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotBlank @Size(max = 100) String title,
    @Size(max = 500) String description,
    TaskStatus status,
    @NotNull TaskPriority priority,
    String assignee) {
  public TaskChanges toChanges() {
    return new TaskChanges(title, description, status, priority, assignee);
  }
}
