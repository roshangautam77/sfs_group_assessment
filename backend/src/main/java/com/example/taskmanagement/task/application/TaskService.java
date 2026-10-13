package com.example.taskmanagement.task.application;

import com.example.taskmanagement.task.domain.Task;
import com.example.taskmanagement.task.domain.TaskChanges;
import com.example.taskmanagement.task.domain.TaskRepository;
import com.example.taskmanagement.task.domain.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TaskService {
  private final TaskRepository repository;
  private final Clock clock;

  public TaskService(TaskRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public Mono<Task> create(TaskChanges input) {
    return Mono.defer(
        () -> {
          Instant now = clock.instant();
          return repository.create(
              new Task(
                  UUID.randomUUID().toString(),
                  input.title().strip(),
                  input.description(),
                  input.status() == null ? TaskStatus.TODO : input.status(),
                  input.priority(),
                  normalizedAssignee(input.assignee()),
                  now,
                  now));
        });
  }

  public Flux<Task> list(TaskStatus status) {
    return repository.findAll(status);
  }

  public Mono<Task> get(String id) {
    return repository.findById(id).switchIfEmpty(Mono.error(TaskNotFoundException::new));
  }

  public Mono<Task> update(String id, TaskChanges input) {
    return repository
        .update(
            id,
            existing ->
                new Task(
                    existing.id(),
                    input.title().strip(),
                    input.description(),
                    input.status(),
                    input.priority(),
                    normalizedAssignee(input.assignee()),
                    existing.createdAt(),
                    laterOf(clock.instant(), existing.updatedAt())))
        .switchIfEmpty(Mono.error(TaskNotFoundException::new));
  }

  public Mono<Void> delete(String id) {
    return repository
        .deleteById(id)
        .flatMap(deleted -> deleted ? Mono.empty() : Mono.error(new TaskNotFoundException()));
  }

  private static String normalizedAssignee(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static Instant laterOf(Instant now, Instant previous) {
    return now.isBefore(previous) ? previous : now;
  }
}
