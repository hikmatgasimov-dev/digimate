# DigiMate test coverage and next steps

Updated: **12 November 2026**.

The project currently has **32 test classes**. The latest full run completed
**266 test executions with no failures, errors or skipped tests**. This includes
seven API E2E scenarios. A parameterized test runs once for each input, so the
execution count is higher than the number of test methods.

`UserServiceTest`, `HelpRequestResponseTest`, `CustomUserDetailsServiceTest`,
`UserRepositoryTest`, `ReviewRepositoryTest` and `GlobalExceptionHandlerTest`
are implemented. They are no longer work waiting to be added.

This document describes the coverage we have now. Useful additions are listed
separately as deferred work because there is not enough time to include them
in the current scope. Unit-test details are in [UNIT_TESTS.md](UNIT_TESTS.md).

## 1. What the tests cover

| Test group | Classes | Executions | Main purpose |
|---|---:|---:|---|
| Unit tests | 12 | 119 | Service rules, DTO validation, response mapping, user details and OpenAPI configuration |
| MVC and form-login tests | 13 | 119 | HTTP responses, validation, page models, redirects, error handling and selected access checks |
| PostgreSQL repository tests | 3 | 14 | Stored data, repository queries and selected uniqueness constraints |
| JWT integration tests | 1 | 6 | Real login, token authentication and selected protected endpoints |
| API E2E tests | 2 | 7 | Complete backend journeys through HTTP and PostgreSQL |
| Application startup test | 1 | 1 | Spring application context starts with the test profile |
| **Total** | **32** | **266** | |

### HTTP and page coverage

The seven API controller test classes cover authentication, categories,
requests, helper actions, reviews, complaints and administration. Services are
mocked in these MVC tests: they check HTTP behavior, not the real service rules.
For example, a mocked service exception can verify a 403 response without
proving that the real service rejects that business action.

The page tests cover the following areas:

- `AuthPageControllerTest`: home-page redirects, guest access to styles and
  rejection of normal users on admin pages.
- `RequestPageControllerTest`: review and complaint forms, their success/error
  feedback, selected validation and CSRF checks, and admin restrictions on
  creating requests. Broader create/edit/cancel form coverage is still missing.
- `HelperPageControllerTest`: inbox data, acceptance/completion feedback,
  helper eligibility messages and a missing-CSRF rejection.
- `AdminPageControllerTest`: helper/user lists, verification, removal, complaint
  dismissal, validation and selected access/CSRF checks.
- `LoginRedirectTest`: real form-login processing with in-memory users,
  including successful login and a wrong password.

`GlobalExceptionHandlerTest` checks status codes and error messages, including
protection against exposing internal exception, authentication and database
details. These tests do not replace a browser smoke check.

### Database and authentication coverage

- `HelpRequestRepositoryTest` saves and reloads an OPEN request, including its
  relationships, creation time and version.
- `UserRepositoryTest` checks email lookup/existence, pending-helper filtering
  and duplicate stored emails.
- `ReviewRepositoryTest` checks lookup/existence by request and rejects a
  second review for the same request.
- `JwtLoginIntegrationTest` uses PostgreSQL users and real API login. It checks
  admin access, normal-user rejection, missing/broken tokens and failed login.
- `DigimateApplicationTests` checks application startup.

A database uniqueness test does not prove the outcome of two concurrent HTTP
requests. Concurrent acceptance, reviews and complaints remain deferred.

## 2. The seven API E2E scenarios

[ApiLifecycleE2ETest](../../src/test/java/com/digimate/controller/api/ApiLifecycleE2ETest.java)
contains E2E-01 to E2E-03.
[RequestActionsE2ETest](../../src/test/java/com/digimate/controller/api/RequestActionsE2ETest.java)
contains E2E-04 to E2E-07.

| Case | Journey | What is checked |
|---|---|---|
| E2E-01 | Registration to completion | Customer/helper registration, admin verification, request creation, acceptance and completion; lists, contacts, ownership and saved states |
| E2E-02 | Acceptance after verification | A PENDING helper is rejected without changing the request, then accepts successfully after admin verification |
| E2E-03 | Two sequential acceptance attempts | The first helper keeps the assignment; the second gets 409 and cannot complete that request |
| E2E-04 | Edit and cancel | Edited fields are returned and stored; ownership is retained; cancellation keeps the row and removes the request from available work |
| E2E-05 | Review completed work | Rating and trimmed comment are stored and read back; a duplicate gets 409 and the original review remains unchanged |
| E2E-06 | Report and dismiss a complaint | An owner reports ACCEPTED work; the admin sees and dismisses it; duplicate reports before and after dismissal are rejected |
| E2E-07 | Remove a helper | Accepted work is released, completed work is preserved, open complaints are closed and selected unrelated data stays unchanged; another helper can accept the released work |

E2E-07 also checks that the removed helper cannot accept work again and that
repeated removal, re-verification and dismissal of a removed complaint are
rejected without changing stored data. Its assertions are now grouped into
private helper methods; it remains one test with the same checks.

Both classes share `HttpE2ETestSupport`. They use a random server port, real
controllers/services/security, API-issued JWTs and disposable PostgreSQL.
Fixtures prepare starting conditions; the actions under test use HTTP. Fresh
JDBC reads confirm saved results and unchanged data after rejected actions.
Run database test classes sequentially.

The agreed scope is these seven journeys. The former standalone INT tests
and controlled acceptance race are not part of the current E2E classes.
E2E-03 is a sequential conflict check, not a concurrency test.

### Contact visibility

The expected contact rules are:

- OPEN and CANCELLED requests expose no counterpart name, phone or email.
- For ACCEPTED and COMPLETED requests, each participant can see the other
  participant's contacts.
- Unrelated viewers and admins who are not participants receive no counterpart
  contacts. Permission to read the request itself is a separate check.

`HelpRequestResponseTest` covers the mapping combinations; the E2E journeys
check selected responses through HTTP. Browser routes and form behavior are
specified in [PAGE_CONTRACT.md](../PAGE_CONTRACT.md).

## 3. Known gaps and useful additions

These items are **deferred because of the current time limit**. They are not
claims of existing coverage or commitments for this branch.

| Area | Useful next step |
|---|---|
| Creation time in HTTP responses | Strengthen the shared `createdAt` assertion to require the field and a valid time value. The current null check can miss an absent field |
| Form data passed to services | Check the exact review/complaint DTOs in successful page-controller tests; the current matchers accept any DTO of the right type |
| Removed-helper verification | Add the missing `REMOVED` case to `UserServiceTest`. Re-verification after removal is already checked in E2E-07 |
| Service and DTO boundaries | Add the remaining focused rejection and validation cases listed in [UNIT_TESTS.md](UNIT_TESTS.md) |
| Concurrent actions | Check simultaneous acceptance and duplicate review/complaint submissions, including the final stored result |
| Transaction failures | Check rollback when a multi-step action fails partway through |
| Access and state combinations | Extend real-HTTP checks for foreign ownership, invalid states, admin-only actions and contact visibility beyond the selected journeys |
| Browser behavior | Add wider form/navigation checks, accessibility and date/time display checks; browser automation can follow later |

Unicode password byte-limit behavior still needs a contract decision before
adding assertions for the expected behavior. Existing password boundary tests
use ASCII input. No coverage-percentage threshold is configured.

## 4. Later refactoring and test names

Broader test refactoring and naming improvements are planned for a **separate
branch later**.

Useful improvements include clearer method names, readable `@DisplayName`
labels where helpful, and parameterized tests for repeated examples of the
same rule. The seven E2E tests already have `@DisplayName` labels.

Two API MVC test names currently describe an admin-create restriction and a
missing in-person place, while their mocks actually check exception-to-HTTP
mapping. Align those names with what is tested. Keep all existing scenarios
and assertions when reorganizing tests; do not combine unrelated behaviors
only to reduce the number of methods.

## 5. Latest verified result

The full Maven run finished on **12 November 2026 at 20:23 Europe/Riga**:
**266 tests, 0 failures, 0 errors, 0 skipped**. The focused `ReviewServiceTest`
run also passed all six tests.

The run used Java 21, a temporary project copy and a separate PostgreSQL
`digimate_test` database. The current source and build files were compared with
that tested copy and match. The recorded source is the working tree based on
`3a70bf7`, including local changes, rather than that commit alone.

SonarQube, remote CI and the browser smoke check were not run in this
verification. The automated result does not establish those checks as passed.

## 6. How to verify changes

1. Use Java 21 and an isolated, disposable PostgreSQL database. Set `DB_USER`
   and `DB_PASSWORD` as described in [README.md](../../README.md#running-tests).
   The test profile uses `create-drop`; do not run an application against the
   same database while tests are running.
2. Preserve any reports you need, then establish a clean baseline with
   `./mvnw clean test` before changing test code.
3. During the change, run the affected class, for example
   `./mvnw -Dtest=ReviewServiceTest test`. For the E2E pair, use
   `./mvnw -Dtest=ApiLifecycleE2ETest,RequestActionsE2ETest test`.
4. Run `./mvnw clean test` for the final source state. Surefire and JaCoCo
   reports in `target/` belong to the run that generated them; focused-run
   coverage is not full-suite coverage.
5. Perform the browser smoke check: a customer creates a request, a helper
   accepts/completes it, and an admin verifies or removes a helper. Use separate
   sessions. The PR template also requires trying the app through Swagger or
   a browser; the coding guide requires tests before a PR.
6. Record the tested source, environment, results and any blocked or unperformed
   checks. Keep passing automated tests separate from manual and remote results.
