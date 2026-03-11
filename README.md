# Feedback

> **This project was fully developed using the [AI Unified Process](https://unifiedprocess.ai)** — a spec-driven software
> development methodology that guides the entire lifecycle from requirements to deployment.

A feedback collection application for events, built with Spring Boot, Vaadin, and jOOQ.

## Tech Stack

- **Java 25** / **Spring Boot 4.0.3**
- **Vaadin 25** - Server-side Java UI framework
- **jOOQ** - Type-safe SQL queries, code generated from Flyway migration scripts
- **PostgreSQL 17** - Database
- **Flyway** - Database migrations (`src/main/resources/db/migration/`)
- **Spring Security** - Passwordless email-based one-time-token authentication

## Prerequisites

- Java 25 (Azul Zulu or equivalent)
- Docker (required for Testcontainers)

## Building

```bash
./mvnw clean package
```

jOOQ classes are generated during the build from the Flyway DDL scripts — no running database needed.

## Running for Development

Use the `TestApplication` entry point which starts the app with Testcontainers (PostgreSQL + Mailpit):

```bash
./mvnw spring-boot:test-run
```

This automatically starts:

- A **PostgreSQL 17** container (database)
- A **Mailpit** container (fake SMTP server for email testing)

### Mailpit

Mailpit captures all outgoing emails (e.g. login codes). On startup, Testcontainers logs the mapped host and port for
the Mailpit web UI — look for a log line like:

```
Mapped port 8025 -> <host-port>
```

Open `http://localhost:<host-port>` in your browser to view captured emails. This is essential for testing the
passwordless login flow, where a one-time code is sent via email.

The Mailpit container is configured in `src/test/java/ch/martinelli/feedback/TestContainersConfiguration.java` and uses
Spring Boot's `@ServiceConnection` for automatic SMTP configuration.

## Running Tests

```bash
./mvnw test
```

### Testcontainers

All integration tests use [Testcontainers](https://www.testcontainers.org/) — **Docker must be running**.

Two containers are started automatically:

| Container  | Image                     | Purpose                           |
|------------|---------------------------|-----------------------------------|
| PostgreSQL | `postgres:17`             | Integration test database         |
| Mailpit    | `axllent/mailpit:v1.28.3` | Captures emails sent during tests |

Container configuration lives in `TestContainersConfiguration.java`. Spring Boot's `@ServiceConnection` handles all
connection properties automatically.

### Test Framework

Tests use [Karibu Testing](https://github.com/nicknisi/karibu-testing) for server-side Vaadin UI testing. The base class
`KaribuTest` provides:

- MockVaadin setup/teardown
- `login()` / `logout()` helpers with role assignment
- Spring context access

There are 16 use case tests (`UC01`–`UC16`) covering the full application workflow. Each test is annotated with
`@UseCase` for traceability back to the specifications in `docs/use_cases/`.

## Code Coverage

```bash
./mvnw clean verify -Pcoverage
```

Generates a JaCoCo report in `target/site/jacoco/`.

## Deployment

A `Dockerfile` is provided using Azul Zulu JDK 25. The app is configured for deployment on Fly.io (`fly.toml`).

## Project Structure

```
src/main/java/ch/martinelli/feedback/
├── auth/         # Passwordless email authentication
├── form/         # Form creation, editing, publishing, sharing, templates
└── response/     # Feedback submission and result analysis/export

src/main/resources/
├── db/migration/ # Flyway migrations (V1–V5)
└── vaadin-i18n/  # Translations

src/test/java/ch/martinelli/feedback/
├── KaribuTest.java                    # Base test class
├── TestApplication.java               # Dev entry point with Testcontainers
├── TestContainersConfiguration.java   # Container definitions
└── usecases/                          # UC01–UC16 test classes

docs/
├── entity_model.md      # ER diagram and entity specs
└── use_cases/           # Use case specifications
```

## License

Apache License 2.0
