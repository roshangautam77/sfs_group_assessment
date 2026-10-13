package com.example.taskmanagement.task.api;

import com.example.taskmanagement.task.application.TaskService;
import com.example.taskmanagement.task.domain.TaskStatus;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  private final TaskService service;

  public TaskController(TaskService service) {
    this.service = service;
  }

  @PostMapping
  public Mono<ResponseEntity<TaskResponse>> create(@Valid @RequestBody CreateTaskRequest input) {
    return service
        .create(input.toChanges())
        .map(TaskResponse::from)
        .map(task -> ResponseEntity.created(URI.create("/api/tasks/" + task.id())).body(task));
  }

  @GetMapping
  public Flux<TaskResponse> list(@RequestParam(required = false) TaskStatus status) {
    return service.list(status).map(TaskResponse::from);
  }

  @GetMapping("/{id}")
  public Mono<TaskResponse> get(@PathVariable String id) {
    return service.get(id).map(TaskResponse::from);
  }

  @PutMapping("/{id}")
  public Mono<TaskResponse> update(
      @PathVariable String id, @Valid @RequestBody UpdateTaskRequest input) {
    return service.update(id, input.toChanges()).map(TaskResponse::from);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(@PathVariable String id) {
    return service.delete(id);
  }
}
