package com.example.taskmanagement.task.domain;

public record TaskChanges(
    String title, String description, TaskStatus status, TaskPriority priority, String assignee) {}
