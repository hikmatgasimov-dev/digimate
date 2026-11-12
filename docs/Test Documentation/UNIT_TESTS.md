# DigiMate unit test coverage

Updated: **12 November 2026**.

The project has **12 unit-test classes with 119 test executions** in the latest
successful full run. These tests exercise application code without starting
the Spring application. Dependencies such as repositories
and the password encoder are mocked where needed.

`UserServiceTest`, `HelpRequestResponseTest` and
`CustomUserDetailsServiceTest` are already implemented. The sections below
separate their current coverage from useful work left for later.

See [TEST_CASES.md](TEST_CASES.md) for MVC, PostgreSQL, JWT, E2E and browser
checks, as well as the full-suite result of 266 passing tests. Execution counts
include each input of a parameterized test.

## 1. What is covered now

| Class | Executions | Main checks |
|---|---:|---|
| `UserServiceTest` | 13 | Registration fields, encoded password, normalized/duplicate email, helper verification and pending-helper lists |
| `HelpRequestResponseTest` | 26 | Request fields, participant contact visibility, viewer identity and optional review data |
| `CustomUserDetailsServiceTest` | 7 | Stored credentials, USER/ADMIN authorities, unknown email and separation of helper status from authorities |
| `HelpRequestServiceTest` | 13 | Create/read/update/cancel, missing category, required in-person place, ownership/state failures and admin-create rejection |
| `HelperFlowServiceTest` | 8 | Available/assigned requests, acceptance/completion and selected helper eligibility failures |
| `ReviewServiceTest` | 6 | Successful review, blank-comment normalization, missing request, wrong reviewer, unfinished request and duplicate review |
| `ComplaintServiceTest` | 4 | Complaint creation, restricted admin response fields, dismissal and customer complaint-status mapping |
| `CategoryServiceTest` | 1 | Category listing uses the repository query ordered by name |
| `RegisterRequestTest` | 24 | Required registration fields, email format and declared length boundaries |
| `HelpRequestDtoTest` | 3 | A normal description is accepted; card-like and ID-like numbers are rejected |
| `NoSensitiveDataValidatorTest` | 12 | Representative allowed text and rejected sensitive-number examples |
| `OpenApiConfigTest` | 2 | Error response schemas, preservation of success responses and bodyless 401 documentation |
| **Total** | **119** | |

These tests establish the behavior of the code they call. Mocked repositories
do not prove SQL queries, database constraints or committed transactions; those
belong to the repository and integration tests listed in the main document.

## 2. Registration and helper verification

`UserServiceTest` currently checks:

- Duplicate emails are rejected, including different letter casing, without
  saving a user or encoding a password.
- Registration stores the encoder result, normalizes email and assigns `USER`.
- Customers start with helper status `NONE`; applicants start with `PENDING`.
- A PENDING helper becomes VERIFIED and the response reflects the new status.
- A missing user is rejected. Verification from `NONE` or `VERIFIED` is rejected
  while preserving the checked role/status and without an explicit save.
- The pending-helper list is mapped correctly, including an empty list.

**Remaining gap:** there is no unit case for verification from `REMOVED`.
E2E-07 already checks that an admin cannot re-verify a removed helper, so this
behavior is not completely untested. A focused unit case would complete the
planned invalid-status set and make a failure easier to diagnose.

These unit tests do not prove ADMIN access enforcement or database persistence.
A managed entity can be updated without an explicit repository `save`, so a
successful verification test should focus on the changed state and response.

## 3. Response mapping and user details

`HelpRequestResponseTest` checks request-field mapping and review fields with
and without a review. Contact checks include:

- Both participants in ACCEPTED and COMPLETED requests.
- Hidden contacts for OPEN/CANCELLED requests and non-participant viewers.
- Null viewers, viewers without an ID and requests without an assigned helper.
- Recognition of the same customer ID across different entity instances.

The mapper tests cover field visibility. Authorization to read the request
belongs to service and HTTP tests.

`CustomUserDetailsServiceTest` checks the stored username/password hash,
`ROLE_USER`, `ROLE_ADMIN` and rejection of an unknown email. It also checks
that PENDING/VERIFIED helper status does not add a separate helper authority.
This class is implemented and is no longer a deferred addition.

## 4. Useful additions when time allows

The following work is **deferred**, not required for the current limited scope.
The earlier UNIT identifiers are retained only as references to the coverage
areas; existing tests in those areas must be kept.

| Area | Useful additions |
|---|---|
| Registration and validation — UNIT-01/02/04/05 | More validator examples and complete help-request DTO boundaries beyond the existing description checks. Resolve the Unicode password byte-limit contract before asserting it |
| Helper verification — UNIT-06 | Rejection of `REMOVED`, preserving the user's state |
| Request services — UNIT-08–12 | Remaining missing-user/request cases and combinations of ownership, state and required place that are not covered yet |
| Helper actions — UNIT-13–15 | More focused self-acceptance, wrong-helper completion and invalid-state cases using the real service |
| Reviews — UNIT-16/17 | Dedicated review DTO boundaries and remaining comment/rejection combinations |
| Complaints and moderation — UNIT-18–20 | Negative complaint-service cases, moderation DTO boundaries and focused helper-removal service tests |

`CustomUserDetailsServiceTest` and the planned response-mapping groups are
already present. Database email/review uniqueness also has repository tests.
Concurrent requests and forced transaction rollback remain integration/E2E
work rather than additions to these unit classes.

## 5. Test maintenance and later refactoring

The four exception tests in `ReviewServiceTest` now prepare `ReviewDto` before
`assertThrows`. Each lambda contains only the service call being checked.
The expected exceptions and checks that no review is saved are unchanged.

Broader cleanup is planned for a **separate branch later**:

- Align method names with the behavior actually checked and add readable
  `@DisplayName` labels where they improve the test report.
- Consider parameterizing repeated inputs for the same rule, such as invalid
  helper statuses. Keep separate tests for different registration properties.
- Give independent examples their own results, for example the two sensitive
  complaint-text examples currently checked in a loop in a page MVC test.
- Extract repeated preparation or assertions only when it makes the scenario
  easier to follow. Preserve all existing coverage during refactoring.

## 6. Writing and running unit tests

- Use Arrange, Act, Assert and fresh test entities.
- Mock dependencies, not the service or mapper being tested. Keep strict stubbing.
- Prepare DTOs and other arguments before an exception assertion. Leave only
  the operation expected to fail inside its lambda.
- Check business fields, expected exceptions and meaningful absence of writes.
  Avoid checking every internal call without a behavioral reason.
- Run the affected class while working, for example
  `./mvnw -Dtest=ReviewServiceTest test`.
- Follow the complete verification steps in [TEST_CASES.md](TEST_CASES.md#6-how-to-verify-changes)
  for the final state. Unit-only results do not replace database, E2E or manual
  checks. No numerical coverage threshold is configured.
