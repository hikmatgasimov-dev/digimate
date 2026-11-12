# digimate
Web platform connecting people who need digital help with verified local helpers (Java, Spring Boot, PostgreSQL).

## Live site and demo accounts

Live site: https://team1.acnbootcamp.lv

Use these accounts to try the application. The data on the site is demo data only.

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@digimate.test` | `0OpQby6c3zoHgt` |
| Customer | `customer1@digimate.test` | `0OpQby6c3zoHgt` |
| Customer | `customer2@digimate.test` | `0OpQby6c3zoHgt` |
| Verified helper | `helper@digimate.test` | `0OpQby6c3zoHgt` |

These passwords work on the live site only. On your own computer, see "Demo accounts on your own computer" below.

Log in as the admin to approve helpers, view users and handle reports at `/admin/helpers`.
All demo accounts share one password. The full list of accounts is in [docs/DEMO_DATA.md](docs/DEMO_DATA.md).

### Demo accounts on your own computer

On your own computer the demo accounts get the password that **you** choose. Set `DEMO_PASSWORD`
(8 to 72 characters, for example `Demo12345`) before you start the application.

IntelliJ: Run > Edit Configurations > DigimateApplication > Environment variables:

```
DEMO_PASSWORD=Demo12345
```

Terminal (PowerShell, for this window only):

```powershell
$env:DEMO_PASSWORD = "Demo12345"
```

The log then shows `Demo data ready: 7 users and 6 categories created`, and you can log in with the emails above.

- Without `DEMO_PASSWORD` (or with one shorter than 8 characters) only the categories are created, and there are no demo accounts.
- The accounts are created only the first time. If they already exist in your local database, a new password does not change them. Delete the rows first, for example `DELETE FROM users WHERE email LIKE '%@digimate.test';`, then restart.
- Do not commit your own passwords. The password in the table above is only for the live site.

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
[docs/TEST_CASES.md](docs/Testing%20Strategy/TEST_CASES.md). The list needs Hikmat's review; the README
test-setup instructions also need Mykolas's review, as requested in SCRUM-12.
