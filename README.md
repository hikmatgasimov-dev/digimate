# digimate
Web platform connecting people who need digital help with verified local helpers (Java, Spring Boot, PostgreSQL).

## Running tests

Requirements: Java 21 and a running local PostgreSQL server on `localhost:5432`.
Create a dedicated, disposable database once per machine:

```sql
CREATE DATABASE digimate_test;
```

Set `DB_USER` and `DB_PASSWORD` in the terminal or IDE environment. The PostgreSQL
role must be able to connect to `digimate_test` and create tables in its schema.
Do not commit credentials or use a database containing data you want to keep.

Run the complete suite from the project root:

```sh
./mvnw test
```

On Windows, use `mvnw.cmd test`.

Database tests use `@ActiveProfiles("test")` to load
`src/test/resources/application-test.properties`. Hibernate uses `create-drop`,
so it creates tables when a test application context starts and drops them when
that context closes. The development database `digimate` is configured separately.
JPA slice tests also need `@AutoConfigureTestDatabase(replace = NONE)` to use
PostgreSQL. They roll back their data after each test.

`TestData` in `src/test/java/com/digimate/support` creates fresh, unsaved customer,
verified-helper, pending-helper, category and OPEN-request fixtures. Customize
fields with setters and save dependencies explicitly. Change fixture emails if
one test needs multiple users of the same kind, because emails must be unique.
`HelpRequestRepositoryTest` demonstrates saving and reloading a request.

For future tests, use the Spring Boot 4 packages:

- `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
- `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
- `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- `org.springframework.test.context.bean.override.mockito.MockitoBean`

The Boot 4 test starters in `pom.xml` already supply `spring-boot-starter-test`
and Spring Security test support transitively.

The proposed Sprint 1 test cases and unresolved contract questions are in
[docs/TEST_CASES.md](docs/TEST_CASES.md). The list needs Hikmat's review; the README
test-setup instructions also need Mykolas's review, as requested in SCRUM-12.
