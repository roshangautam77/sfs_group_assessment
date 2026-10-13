package com.example.taskmanagement.shared.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.taskmanagement.task.api.TaskController;
import com.example.taskmanagement.task.application.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class ApiErrorHandlerTest {
  @Test
  void unexpectedErrorsDoNotLeakInternalDetails() {
    TaskService service = mock(TaskService.class);
    when(service.get("id"))
        .thenReturn(Mono.error(new IllegalStateException("Sensitive internal detail")));
    WebTestClient.bindToController(new TaskController(service))
        .controllerAdvice(new ApiErrorHandler())
        .build()
        .get()
        .uri("/api/tasks/id")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo("An unexpected error occurred")
        .jsonPath("$.type")
        .isEqualTo("urn:task-management:error:http-500")
        .jsonPath("$.instance")
        .isEqualTo("/api/tasks/id");
  }
}
