# DigiMate coding guide

## Workflow
- One Jira card = one branch = one pull request.
- Branch: `feature/SCRUM-<n>-<name>`. Commit: `SCRUM-<n> message`.
- Run `./mvnw test` before every pull request (`mvnw.cmd test` on Windows).
- One reviewer who is not the author. Squash-merge. Never push to `main`.

## Code
- Java 21, Spring Boot 4.0.0, Maven, springdoc 3.0.0. PostgreSQL only 
- Layers: controller -> service -> repository. Rules live in services. Controllers only translate HTTP.
- Entities: `@Getter @Setter @NoArgsConstructor`, never `@Data`. Enums with `@Enumerated(EnumType.STRING)`.
- DTOs are records. Validate with `@Valid` and Bean Validation.
- Username is the email. Services receive the username string.
- Errors: custom unchecked exceptions, one `GlobalExceptionHandler`. 404 not found, 403 not allowed, 409 conflict, 400 invalid input.
- Logging: SLF4J (`@Slf4j`), IDs only. Never log passwords, phones, emails or request text.
- Secrets: never in Git. `DB_USER` and `DB_PASSWORD` are environment variables.

## Pages (Thymeleaf)
- Labelled inputs (`for` and `id`), `lang="en"`, one `<h1>` per page, errors as visible text.
- Use the shared layout and `app.css`.

## Tests
- Names say what is checked, for example `shouldRejectAcceptWhenHelperNotVerified`.
- Arrange, Act, Assert. Test failure paths too.