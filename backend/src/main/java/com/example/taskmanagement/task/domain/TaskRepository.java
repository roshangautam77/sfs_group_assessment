package com.example.taskmanagement.task.domain;

import java.util.function.UnaryOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskRepository {
  Mono<Task> create(Task task);

  Flux<Task> findAll(TaskStatus status);

  Mono<Task> findById(String id);

  Mono<Task> update(String id, UnaryOperator<Task> update);

  Mono<Boolean> deleteById(String id);
}
