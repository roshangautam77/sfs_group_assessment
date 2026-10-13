package com.example.taskmanagement.task.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.taskmanagement.task.domain.Task;
import com.example.taskmanagement.task.domain.TaskPriority;
import com.example.taskmanagement.task.domain.TaskStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class InMemoryTaskRepositoryTest {
  private final InMemoryTaskRepository repository = new InMemoryTaskRepository();

  private Task task(String id, String title, TaskStatus status, Instant createdAt) {
    return new Task(id, title, null, status, TaskPriority.LOW, null, createdAt, createdAt);
  }

  @Test
  void mutationsAreLazyAndDuplicateIdsDoNotOverwrite() {
    Task task = task("a", "Original", TaskStatus.TODO, Instant.EPOCH);
    Mono<Task> pending = repository.create(task);
    StepVerifier.create(repository.findById("a")).verifyComplete();
    StepVerifier.create(pending).expectNext(task).verifyComplete();
    StepVerifier.create(pending).expectError(IllegalStateException.class).verify();
    Mono<Boolean> delete = repository.deleteById("a");
    StepVerifier.create(repository.findById("a")).expectNext(task).verifyComplete();
    StepVerifier.create(delete).expectNext(true).verifyComplete();
    StepVerifier.create(delete).expectNext(false).verifyComplete();
  }

  @Test
  void filtersAndOrdersDeterministically() {
    var old = task("b", "Old", TaskStatus.TODO, Instant.EPOCH);
    var first = task("a", "First", TaskStatus.DONE, Instant.EPOCH.plusSeconds(1));
    var second = task("c", "Second", TaskStatus.DONE, first.createdAt());
    StepVerifier.create(
            Flux.concat(
                repository.create(old), repository.create(second), repository.create(first)))
        .expectNextCount(3)
        .verifyComplete();
    StepVerifier.create(repository.findAll(null)).expectNext(first, second, old).verifyComplete();
    StepVerifier.create(repository.findAll(TaskStatus.DONE))
        .expectNext(first, second)
        .verifyComplete();
  }

  @Test
  void concurrentUpdatesDoNotLoseReadModifyWriteChanges() {
    var original = task("counter", "0", TaskStatus.TODO, Instant.EPOCH);
    StepVerifier.create(
            repository
                .create(original)
                .thenMany(
                    Flux.range(0, 1000)
                        .flatMap(
                            i ->
                                repository
                                    .update(
                                        "counter",
                                        task ->
                                            task(
                                                task.id(),
                                                String.valueOf(Integer.parseInt(task.title()) + 1),
                                                task.status(),
                                                task.createdAt()))
                                    .subscribeOn(Schedulers.parallel()),
                            16))
                .then(repository.findById("counter")))
        .assertNext(task -> assertThat(task.title()).isEqualTo("1000"))
        .verifyComplete();
  }

  @Test
  void updateCannotRecreateDeletedTask() {
    var original = task("a", "Title", TaskStatus.TODO, Instant.EPOCH);
    StepVerifier.create(
            repository
                .create(original)
                .then(repository.deleteById("a"))
                .then(repository.update("a", task -> original)))
        .verifyComplete();
    StepVerifier.create(repository.findById("a")).verifyComplete();
  }
}
