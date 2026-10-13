# Task Management Assessment

A Java 21 / Spring Boot 3.5.16 WebFlux API and an Angular 21 frontend for a small internal task-management feature.

## Run locally

From the repository root, in the first terminal:

```sh
cd backend
./mvnw verify
./mvnw spring-boot:run
```

The API is at `http://localhost:8080/api/tasks`; health is at `http://localhost:8080/actuator/health`.

In a second terminal, from the repository root:

```sh
nvm use                  # if you manage Node with nvm
cd frontend
npm ci
npm test
npm run build
npm start
```

Open `http://localhost:4200`. The development proxy forwards `/api/**` to `http://localhost:8080`, keeping requests on the browser's origin without permissive CORS.

## API contract

| Method | Endpoint | Success response |
| --- | --- | --- |
| POST | `/api/tasks` | 201 with task JSON and a `Location` header |
| GET | `/api/tasks?status=TODO` | 200 with a JSON array; status filter is optional |
| GET | `/api/tasks/{id}` | 200 with task JSON |
| PUT | `/api/tasks/{id}` | 200 with the replacement task JSON |
| DELETE | `/api/tasks/{id}` | 204 with no response body |

`title` is required, cannot be blank, and has a maximum length of 100. `description` is optional and has a maximum length of 500. `priority` is required: `LOW`, `MEDIUM`, or `HIGH`. Creation defaults an omitted/null `status` to `TODO`; an explicit status may be `TODO`, `IN_PROGRESS`, or `DONE`. `assignee` is optional. The frontend defaults priority to `MEDIUM`.

Tasks are listed by creation time descending, then ID ascending for ties. Missing IDs return 404, including repeated deletion. Invalid input or status filters return 400. Errors follow `application/problem+json` with `type`, `title`, `status`, `detail`, and `instance`; validation errors also include an `errors` array of field/message pairs. Unexpected failures expose a generic message, not a stack trace.

Create and list a task:

```sh
curl -i http://localhost:8080/api/tasks   -H 'Content-Type: application/json'   -d '{"title":"Review implementation","priority":"HIGH"}'
curl 'http://localhost:8080/api/tasks?status=TODO'
```

Use the returned ID for the remaining operations:

```sh
TASK_ID='replace-with-returned-id'
curl "http://localhost:8080/api/tasks/$TASK_ID"
curl -X PUT "http://localhost:8080/api/tasks/$TASK_ID"   -H 'Content-Type: application/json'   -d '{"title":"Review complete","status":"DONE","priority":"HIGH"}'
curl -i -X DELETE "http://localhost:8080/api/tasks/$TASK_ID"
```

Example validation error:

```json
{
  "type": "urn:task-management:error:http-400",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/tasks",
  "errors": [{ "field": "title", "message": "must not be blank" }]
}
```

## Structure and architectural decisions

```text
backend/src/main/java/com/example/taskmanagement/
  task/api/             Controllers, validated requests, response mapping
  task/application/     TaskService and use-case errors
  task/domain/          Immutable records, enums, repository contract
  task/infrastructure/  Concurrent in-memory repository
  shared/api/           Central problem-response handling
  config/               Injectable UTC clock
frontend/src/app/
  core/http/            Shared typed HTTP error translation
  features/tasks/
    domain/             Task models, enums, labels
    data-access/        TaskApi HTTP client and feature-scoped TaskStore
    ui/                 Create form and task table
    task-page.*         Feature composition and page interactions
```

| Decision | Reason and tradeoff |
| --- | --- |
| Feature-first layered backend | Keeps task code together while separating transport, use cases, and storage. Domain types have no Spring dependency. It remains one deployable application; separate services would add unnecessary operational complexity here. |
| Separate request, domain, and response types | Validation belongs at the API boundary. Explicit mapping prevents callers from setting IDs/timestamps and allows the contract to evolve independently of internal records. It adds small, deliberate mapping code. |
| Reactor throughout the backend | Controllers, services, and repository operations compose `Mono`/`Flux`; work is deferred until subscription. There is no blocking I/O, manual subscription, or unnecessary thread switching in the application path. A reactive wrapper alone would not make future blocking persistence safe. |
| Immutable records and atomic map operations | `putIfAbsent` avoids overwriting an existing ID; `computeIfPresent` makes update atomic per task and avoids recreating a deleted task. There is no list-wide snapshot isolation or cross-instance consistency. |
| Injectable `Clock` and generated UUIDs | Supports deterministic time tests, UTC timestamps, and nonsequential identifiers. Updated timestamps never move backwards; UUIDs are identifiers, not access-control credentials. |
| Problem Details and strict input decoding | Provides one consumable error shape and rejects unknown fields/invalid enum representations. A shared interceptor maps errors to safe, actionable frontend messages. Strict decoding requires coordinated contract changes. |
| Standalone Angular components and feature state | Lazy routing and `OnPush` components keep the feature isolated. HTTP access stays in `TaskApi`; state and commands stay in page-scoped `TaskStore`; presentation components receive inputs and emit events. A global state library is unnecessary for this feature's size. |
| RxJS for requests, signals for rendering | `switchMap` cancels obsolete filter reads; `exhaustMap` ignores overlapping creates; `finalize` restores saving state. Inner error handling keeps later retries usable. Signals expose read-only state, with `toSignal`/`takeUntilDestroyed` managing lifetime. |
| Confirmed create followed by refresh | Avoids temporary IDs, optimistic rollback, and divergent server/client state. It costs one additional read. The current filter is retained, so a new TODO task may not appear under a DONE filter. |
| Lightweight native controls | Avoids an extra UI dependency while providing labels, semantic table headers, visible focus, responsive layout, and loading/error/empty states. A shared design system can be adopted when product breadth justifies it. |

## Verification and formatting

Backend:

```sh
cd backend
./mvnw verify
./mvnw spotless:apply   # apply Java formatting after changes
```

`verify` runs JUnit/StepVerifier/WebTestClient tests, packages the application, checks Google Java Format through Spotless, and creates a JaCoCo report at `backend/target/site/jacoco/index.html`. The current configuration reports coverage; it does not enforce a percentage threshold.

Frontend:

```sh
cd frontend
npm run format:check
npm test
npm run build
npm run format          # apply formatting after changes
```

Vitest tests run without requiring Chrome. TypeScript and Angular template checks are strict. The production build enforces bundle budgets. Angular disk caching is disabled because its native cache crashed during local Mac verification; optimization and strict compilation remain enabled.

## Limitations and next steps

The in-memory store meets the assessment constraint: data disappears on restart, is local to one process, and has no capacity limit or pagination. Listing sorts all matching tasks, and concurrent lists are weakly consistent. Atomic updates prevent map-level races but do not detect stale client edits; the last applied update wins.

With additional time and a broader product scope, prioritize:

1. A reactive persistent adapter with migrations and repository contract tests. Revisit the update port for database-native conditional updates, add version/ETag checks for stale edits, and add paginated queries with appropriate indexes.
2. Authentication and server-enforced workspace/role authorization, HTTPS deployment settings, dependency patch management, and edge request/rate limits before external exposure.
3. API schema/contract checks, full browser integration tests, and automated accessibility tests alongside the existing behavioral tests.
4. Correlation IDs, metrics/tracing, and operational dashboards; keep logs free of task descriptions and other unnecessary personal data.
5. Frontend detail/edit/delete workflows only when product requirements call for them, followed by a reusable design system as features grow.
