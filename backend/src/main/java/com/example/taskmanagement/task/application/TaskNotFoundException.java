package com.example.taskmanagement.task.application;

public class TaskNotFoundException extends RuntimeException {
  public TaskNotFoundException() {
    super("Task was not found");
  }
}
