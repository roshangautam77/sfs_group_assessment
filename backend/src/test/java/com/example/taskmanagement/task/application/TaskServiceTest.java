package com.example.taskmanagement.task.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.taskmanagement.task.domain.Task;
import com.example.taskmanagement.task.domain.TaskChanges;
import com.example.taskmanagement.task.domain.TaskPriority;
import com.example.taskmanagement.task.domain.TaskStatus;
import com.example.taskmanagement.task.infrastructure.InMemoryTaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class TaskServiceTest {
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private final InMemoryTaskRepository repository = new InMemoryTaskRepository();
  private final TaskService service = new TaskService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void createsLazilyWithInjectedClock() {
    var pending =
        service.create(new TaskChanges("  Example  ", null, null, TaskPriority.MEDIUM, "  "));
    StepVerifier.create(service.list(null)).verifyComplete();
    StepVerifier.create(pending)
        .assertNext(
            task -> {
              assertThat(task.title()).isEqualTo("Example");
              assertThat(task.createdAt()).isEqualTo(NOW);
              assertThat(task.assignee()).isNull();
              assertThat(task.status()).isEqualTo(TaskStatus.TODO);
            })
        .verifyComplete();
    StepVerifier.create(service.list(null)).expectNextCount(1).verifyComplete();
  }

  @Test
  void neverMovesUpdatedAtBackwards() {
    Task task =
        new Task(
            "id", "Title", null, TaskStatus.TODO, TaskPriority.LOW, null, NOW, NOW.plusSeconds(60));
    StepVerifier.create(repository.create(task)).expectNext(task).verifyComplete();
    StepVerifier.create(
            service.update(
                "id", new TaskChanges("New", null, TaskStatus.DONE, TaskPriority.HIGH, null)))
        .assertNext(
            updated -> {
              assertThat(updated.createdAt()).isEqualTo(NOW);
              assertThat(updated.updatedAt()).isEqualTo(task.updatedAt());
              assertThat(updated.status()).isEqualTo(TaskStatus.DONE);
            })
        .verifyComplete();
  }

  @Test
  void missingTasksSignalDomainError() {
    StepVerifier.create(service.get("missing")).expectError(TaskNotFoundException.class).verify();
    StepVerifier.create(service.delete("missing"))
        .expectError(TaskNotFoundException.class)
        .verify();
  }
}
