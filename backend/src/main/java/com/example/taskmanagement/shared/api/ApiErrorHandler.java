package com.example.taskmanagement.shared.api;

import com.example.taskmanagement.task.application.TaskNotFoundException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class ApiErrorHandler extends ResponseEntityExceptionHandler {
  public record FieldViolation(String field, String message) {}

  @ExceptionHandler(TaskNotFoundException.class)
  public Mono<ResponseEntity<Object>> notFound(
      TaskNotFoundException exception, ServerWebExchange exchange) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    return handleExceptionInternal(
        exception, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, exchange);
  }

  @Override
  protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(
      WebExchangeBindException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      ServerWebExchange exchange) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
    List<FieldViolation> violations =
        exception.getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
            .sorted(
                Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
            .toList();
    problem.setProperty("errors", violations);
    return handleExceptionInternal(exception, problem, headers, status, exchange);
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Object>> unexpected(Exception exception, ServerWebExchange exchange) {
    logger.error("Unexpected API failure at " + exchange.getRequest().getPath(), exception);
    return handleExceptionInternal(
        exception,
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
        new HttpHeaders(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        exchange);
  }

  @Override
  protected Mono<ResponseEntity<Object>> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode status, ServerWebExchange exchange) {
    ProblemDetail problem =
        body instanceof ProblemDetail detail ? detail : ProblemDetail.forStatus(status);
    problem.setType(URI.create("urn:task-management:error:http-" + status.value()));
    problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
    // Framework decoding messages can expose implementation details; provide a stable public
    // contract.
    if (status.value() == 400 && problem.getProperties() == null) {
      problem.setDetail("Invalid request. Check JSON fields, enum values, and query parameters.");
    }
    return super.createResponseEntity(problem, headers, status, exchange);
  }
}
