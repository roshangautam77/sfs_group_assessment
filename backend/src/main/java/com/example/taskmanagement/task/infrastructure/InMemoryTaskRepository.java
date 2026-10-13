package com.example.taskmanagement.task.infrastructure;

import com.example.taskmanagement.task.domain.Task;
import com.example.taskmanagement.task.domain.TaskRepository;
import com.example.taskmanagement.task.domain.TaskStatus;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryTaskRepository implements TaskRepository {
  private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

  @Override
  public Mono<Task> create(Task task) {
    return Mono.fromSupplier(
        () -> {
          if (tasks.putIfAbsent(task.id(), task) != null) {
            throw new IllegalStateException("Generated task ID already exists");
          }
          return task;
        });
  }

  @Override
  public Flux<Task> findAll(TaskStatus status) {
    return Flux.defer(
        () ->
            Flux.fromStream(
                tasks.values().stream()
                    .filter(task -> status == null || task.status() == status)
                    .sorted(
                        Comparator.comparing(Task::createdAt).reversed().thenComparing(Task::id))));
  }

  @Override
  public Mono<Task> findById(String id) {
    return Mono.fromSupplier(() -> tasks.get(id));
  }

  @Override
  public Mono<Task> update(String id, UnaryOperator<Task> update) {
    // Atomic per ID: an update cannot resurrect a concurrently deleted task.
    return Mono.fromSupplier(
        () -> tasks.computeIfPresent(id, (key, existing) -> update.apply(existing)));
  }

  @Override
  public Mono<Boolean> deleteById(String id) {
    return Mono.fromSupplier(() -> tasks.remove(id) != null);
  }
}
