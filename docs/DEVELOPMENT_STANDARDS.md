# Development Standards for Task Management Team

These standards guide new and changed code. Follow established project conventions, keep changes focused, and explain justified exceptions in the PR. Coverage targets are review goals unless an automated gate is explicitly configured.

## Frontend Coding Standards

### Structure and readability

- Organize code by feature and separate presentation, state, data access, and shared utilities. Keep components and functions focused on one responsibility; extract repeated behavior into reusable modules.
- Use descriptive names and consistent formatting. Prefer explicit, meaningful types; do not use `any`. Remove dead code, debug statements, and commented-out code. Use comments to explain intent or tradeoffs, not obvious behavior.

### State and asynchronous operations

- Keep a clear source of truth for each piece of state. Avoid duplicated or directly mutated shared state. Separate derived values from stored values, and keep business rules out of templates.
- Centralize service calls. Handle loading, success, empty, and failure states consistently. Prevent duplicate submissions, ignore obsolete responses, and clean up listeners, timers, and subscriptions when their owner is destroyed.

### Forms and error handling

- Use structured, typed forms with clear labels and validation messages. Validate input before submission, while treating server-side validation as authoritative. Preserve entered data after failure and reset it only after confirmed success.
- Translate service errors into actionable messages through a shared approach. Offer appropriate retry actions; do not silently swallow failures or automatically repeat writes that could create duplicate changes.

### Stylesheets and design

- Follow the existing stylesheet conventions and reuse design tokens for spacing, colors, typography, and breakpoints. Scope feature styles appropriately; avoid duplicated declarations, unnecessary inline styles, routine use of `!important`, and unintended global overrides.
- Use semantic markup, visible keyboard focus, readable contrast, and responsive layouts. Associate errors with fields, announce important status changes, and respect reduced-motion preferences. Never communicate meaning through color alone.

### Safe content and performance

- Render external content safely; avoid raw HTML insertion and bypassing sanitization. Keep credentials and secrets out of client code and logs. Client-side checks improve usability but never replace server authorization.
- Avoid repeated expensive work during rendering and unnecessary network requests. Use stable keys for repeated items, load large features/assets only when needed, and optimize images and bundle size. Measure before adding complexity.

### Tests and documentation

- Write unit and integration tests for changed behavior, including normal, boundary, validation, and failure scenarios. Add or update E2E tests for important affected user journeys. Check supported browsers, narrow layouts, and keyboard accessibility.
- Target at least 80% line coverage for new or changed business logic, while covering critical branches and regressions. Run required quality checks, document relevant behavior/configuration changes, and attach screenshots or recordings to PRs with UI changes.

## Backend Coding Standards

### Structure and readability

- Organize code by feature with clear API, application, domain, and persistence boundaries. Keep business rules separate from transport and storage concerns. Use explicit dependencies, small focused functions, and descriptive names.
- Keep models and contracts strongly typed. Use separate request/response models where needed, map fields deliberately, and avoid exposing storage internals. Prefer simple solutions and meaningful abstractions over unnecessary indirection.

### API contracts and validation

- Define consistent methods, status codes, request/response shapes, and error formats. Document defaults, required fields, update semantics, and compatibility changes. Keep identifiers, timestamps, and other system-managed fields under server control.
- Validate untrusted input at system boundaries, including type, length, range, and allowed values. Reject unexpected fields where the contract requires strict input. Use consistent UTC timestamps and appropriate domain types for dates and numeric values.

### Errors and observability

- Handle errors centrally and distinguish validation failures, missing resources, conflicts, and unexpected failures. Return safe, consistent responses; do not disclose stack traces, internal class names, credentials, or sensitive payloads.
- Use structured logs with useful context and correlation identifiers where available. Choose suitable log levels and avoid logging the same failure repeatedly. Include health checks and operational signals appropriate to the service.

### Data integrity and resource use

- Preserve data integrity with appropriate transactions, uniqueness constraints, and atomic operations. Account for concurrent changes and stale updates. Define idempotent behavior where repeated requests could otherwise duplicate effects.
- Use parameterized database queries and explicit resource cleanup. Apply appropriate pagination, timeouts, payload limits, and connection limits as the service grows. Avoid unbounded work or memory growth; measure performance before optimizing.

### Security and configuration

- Enforce authentication and authorization on the server when required by the application scope. Check access to each protected resource; identifiers and hidden UI controls are not permission checks. Use secure transport for deployed services.
- Keep configuration separate from code and secrets in approved environment/secret-management facilities. Do not commit credentials. Validate essential startup configuration, keep dependencies patched, and assess dependency changes for security and compatibility.

### Tests and documentation

- Write unit, integration, and API contract tests for changed behavior. Cover valid and invalid inputs, boundaries, missing resources, failure paths, and relevant concurrency scenarios. Add a regression test for each defect; keep tests deterministic and independent.
- Target at least 80% line coverage for new or changed business logic and cover critical branches. Run required formatting, static analysis, tests, and build checks. Update API/setup documentation and record any migration, compatibility, or operational implications.

