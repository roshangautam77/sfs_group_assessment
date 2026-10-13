package com.example.taskmanagement.task.domain;

import java.time.Instant;

public record Task(
    String id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    String assignee,
    Instant createdAt,
    Instant updatedAt) {}
