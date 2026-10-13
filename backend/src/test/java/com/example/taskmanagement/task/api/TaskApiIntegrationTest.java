package com.example.taskmanagement.task.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class TaskApiIntegrationTest {
  @Autowired WebTestClient client;

  private TaskResponse create(String title) {
    return client
        .post()
        .uri("/api/tasks")
        .bodyValue(Map.of("title", title, "priority", "HIGH"))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .exists("Location")
        .expectBody(TaskResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void fullCrudLifecycle() {
    TaskResponse created = create("  Ship assessment  ");
    assertThat(created.id()).isNotBlank();
    assertThat(created.title()).isEqualTo("Ship assessment");
    assertThat(created.status().name()).isEqualTo("TODO");
    assertThat(created.createdAt()).isEqualTo(created.updatedAt());
    client
        .get()
        .uri("/api/tasks/{id}", created.id())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(created.id());
    TaskResponse updated =
        client
            .put()
            .uri("/api/tasks/{id}", created.id())
            .bodyValue(
                Map.of(
                    "title",
                    "Ship it",
                    "description",
                    "Reviewed",
                    "priority",
                    "LOW",
                    "status",
                    "DONE",
                    "assignee",
                    "  Alex  "))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(TaskResponse.class)
            .returnResult()
            .getResponseBody();
    assertThat(updated.createdAt()).isEqualTo(created.createdAt());
    assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    assertThat(updated.assignee()).isEqualTo("Alex");
    client
        .get()
        .uri("/api/tasks?status=DONE")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(TaskResponse.class)
        .value(
            tasks -> {
              assertThat(tasks).extracting(TaskResponse::id).contains(created.id());
              assertThat(tasks).allMatch(task -> task.status().name().equals("DONE"));
            });
    client
        .get()
        .uri("/api/tasks")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(TaskResponse.class)
        .value(tasks -> assertThat(tasks).extracting(TaskResponse::id).contains(created.id()));
    client
        .delete()
        .uri("/api/tasks/{id}", created.id())
        .exchange()
        .expectStatus()
        .isNoContent()
        .expectBody()
        .isEmpty();
    client
        .get()
        .uri("/api/tasks/{id}", created.id())
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(404)
        .jsonPath("$.detail")
        .isEqualTo("Task was not found");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\t\n"})
  void rejectsBlankTitles(String title) {
    client
        .post()
        .uri("/api/tasks")
        .bodyValue(Map.of("title", title, "priority", "MEDIUM"))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.errors[0].field")
        .isEqualTo("title")
        .jsonPath("$.detail")
        .isEqualTo("Request validation failed");
  }

  @Test
  void acceptsMaximumLengthsAndRejectsOverflow() {
    TaskResponse created =
        client
            .post()
            .uri("/api/tasks")
            .bodyValue(
                Map.of("title", "x".repeat(100), "description", "x".repeat(500), "priority", "LOW"))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(TaskResponse.class)
            .returnResult()
            .getResponseBody();
    client
        .post()
        .uri("/api/tasks")
        .bodyValue(
            Map.of("title", "x".repeat(101), "description", "x".repeat(501), "priority", "LOW"))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.errors.length()")
        .isEqualTo(2);
    client.delete().uri("/api/tasks/{id}", created.id()).exchange().expectStatus().isNoContent();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"title\":\"A\"}",
        "{\"title\":null,\"priority\":\"LOW\"}",
        "{\"title\":\"A\",\"priority\":\"URGENT\"}",
        "{\"title\":\"A\",\"priority\":0}",
        "{\"title\":\"A\",\"priority\":\"LOW\",\"unknown\":true}",
        "{",
        ""
      })
  void invalidRequestsUseProblemContract(String json) {
    client
        .post()
        .uri("/api/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(json)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.type")
        .isEqualTo("urn:task-management:error:http-400")
        .jsonPath("$.instance")
        .isEqualTo("/api/tasks");
  }

  @Test
  void invalidFilterUsesProblemContract() {
    client
        .get()
        .uri("/api/tasks?status=INVALID")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400);
  }

  @Test
  void updateRequiresStatusAndPriority() {
    TaskResponse created = create("Update validation");
    client
        .put()
        .uri("/api/tasks/{id}", created.id())
        .bodyValue(Map.of("title", "Invalid"))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.errors.length()")
        .isEqualTo(2);
    client.delete().uri("/api/tasks/{id}", created.id()).exchange().expectStatus().isNoContent();
  }

  @Test
  void missingUpdatesAndDeletesReturn404() {
    client
        .put()
        .uri("/api/tasks/missing")
        .bodyValue(Map.of("title", "A", "priority", "LOW", "status", "TODO"))
        .exchange()
        .expectStatus()
        .isNotFound();
    client.delete().uri("/api/tasks/missing").exchange().expectStatus().isNotFound();
  }

  @Test
  void frameworkErrorsUseTheSameProblemContract() {
    client
        .patch()
        .uri("/api/tasks/missing")
        .exchange()
        .expectStatus()
        .isEqualTo(405)
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(405);
    client
        .post()
        .uri("/api/tasks")
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue("text")
        .exchange()
        .expectStatus()
        .isEqualTo(415)
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(415);
  }
}
