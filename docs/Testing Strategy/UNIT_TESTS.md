# DigiMate unit testing checklist

Snapshot: **3 November 2026**. Use with [TEST_CASES.md](TEST_CASES.md).
This is a checklist of coverage to review or add, not a record of passing tests.
Each UNIT ID groups related cases; implement separate tests for distinct rules.

## 1. What a unit test should prove

A unit test checks a real validator, mapper or service method with controlled
inputs. It normally runs without starting Spring, an HTTP server or PostgreSQL.
For a service, use real DTOs/entities and mock its repository dependencies.
The method containing the rule must remain real.

For each case, write down:

1. **Arrange:** actor, helper eligibility, request state, ownership and relevant
   repository answers. Keep unrelated fields valid.
2. **Act:** call the real method once.
3. **Assert:** returned values, changed business fields or the agreed exception.
   On rejection, check that no forbidden save/delete is requested.

Example: for a PENDING helper accepting an OPEN request, prepare that user and
request, call acceptance, and expect rejection with no assignment saved. Testing
the actual HTTP 403 and unchanged database belongs in integration coverage.

## 2. What is available now

| Target | Current state | What the tester can do |
|---|---|---|
| `NoSensitiveDataValidator` | Real implementation; parameterized tests exist | Review UNIT-01 and preserve the existing accepted/rejected examples |
| `HelpRequestDto` | Real constraints; three description tests exist | Add the missing UNIT-02 field boundaries |
| `CustomUserDetailsService` | Real implementation; no unit tests in this checkout | Add UNIT-03 with a mocked `UserRepository` |
| `Request service` | Five methods are placeholders | Prepare cases; test the real implementation after its feature arrives |
| `Registration/verification/helper/review services and registration DTOs` | Not present locally | Use the reviewed requirements below when their implementations arrive |
| `HelpRequestResponse` | Record exists; planned `from` mapper is absent | UNIT-07 depends on the SCRUM-22 mapper implementation |

Recheck incoming feature tests before adding new ones. A missing method is a
dependency, not a reason to mock that method and report its rule as covered.

## 3. Available validators and account lookup

### UNIT-01 — Sensitive-number validator (P0)

Target: [NoSensitiveDataValidator.java](../../src/main/java/com/digimate/validation/NoSensitiveDataValidator.java).
Existing tests: [NoSensitiveDataValidatorTest.java](../../src/test/java/com/digimate/validation/NoSensitiveDataValidatorTest.java).

| Input | Expected result |
|---|---|
| Ordinary help text, a time such as `15:00`, or a short number | Accepted |
| A contiguous 12-digit number | Accepted; below the current card-pattern boundary |
| Synthetic card-like numbers of 13 and 19 digits | Rejected; current lower/upper boundaries |
| Card-like number with spaces or hyphens; synthetic six-digit/five-digit ID pattern | Rejected |
| Null | Accepted by this validator; a required-field constraint rejects it where applicable |

This validator detects the agreed patterns. It does not establish that arbitrary
text contains no secrets or personal information. New pattern rules need an
agreed requirement before setting their expected results.

### UNIT-02 — Help-request DTO fields (P0)

Target: [HelpRequestDto.java](../../src/main/java/com/digimate/dto/HelpRequestDto.java).
Existing tests: [HelpRequestDtoTest.java](../../src/test/java/com/digimate/dto/HelpRequestDtoTest.java).
Call Bean Validation on a real DTO and check the affected property name.
Start with a valid DTO, then vary one input so another error cannot mask it.

| Field | Cases and expected result |
|---|---|
| `title` | Null/empty/whitespace rejected; 100 characters accepted, 101 rejected |
| `description` | Null/empty/whitespace rejected; 1000 accepted, 1001 rejected; ordinary text accepted; synthetic sensitive-number examples rejected on `description` |
| `categoryId` | Null rejected. Whether a supplied ID exists requires service/integration checks |
| `preferredTime` | Null/past rejected; future accepted. Use stable times safely away from the clock boundary |
| `mode` | Null rejected; ONLINE and IN_PERSON accepted. Unparseable JSON enum text is an API check |
| `place` | Null/empty accepted; 255 characters accepted, 256 rejected. No mandatory-place rule is agreed |

For long valid text, avoid accidentally including a sensitive-number pattern.
These checks do not verify browser `datetime-local` binding or database columns.

### UNIT-03 — User lookup for authentication (P0)

Target: [CustomUserDetailsService.java](../../src/main/java/com/digimate/config/CustomUserDetailsService.java).
Prepare a real `User`; mock only `UserRepository`.

| Repository answer | Expected result |
|---|---|
| Existing USER | Returned username is the user's email, password value is the stored hash, authority is ROLE_USER |
| Existing ADMIN | Authority is ROLE_ADMIN |
| No user with that email | `UsernameNotFoundException` |

HelperStatus is separate from role: PENDING/VERIFIED users do not acquire an
invented ROLE_HELPER. A successful lookup does not prove BCrypt authentication;
exercise that through the real security chain in INT-02 of the main plan.

## 4. Registration and verification after implementation

Requirements: SCRUM-23/24. Registration input is `fullName`, `email`, `phone`,
`password`, `wantsToHelp`; the agreed service returns `UserResponse`.

### UNIT-04 — Registration DTO (P0)

| Field | Cases and expected result |
|---|---|
| Required text fields | Null/empty/whitespace rejected |
| `fullName` | 100 characters accepted; 101 rejected |
| `email` | Valid/invalid format; 255/256 length boundary. Keep other constraints satisfied when isolating the length rule |
| `phone` | Required; 30 characters accepted, 31 rejected. Add a format expectation only if agreed |
| `password` | 7 rejected, 8 accepted; 72 accepted and 73 rejected under the declared character limit |

Unicode passwords need a decision: the size annotation counts characters, while
BCrypt limits bytes. Do not silently choose a non-ASCII outcome. `wantsToHelp`
is a boolean preference; role and verification are server decisions.

| ID / priority | Prepare and call | What to assert |
|---|---|---|
| UNIT-05 / P0 — registration service | Register with `wantsToHelp` false/true; prepare absent/existing normalized email; make the password encoder return a test hash | Saved role is USER; helper status is NONE/PENDING; email is normalized; saved password is the encoder result; duplicate normalized email is rejected without saving. Response maps only `id`, `fullName`, `email`, `role`, `helperStatus`, with no password/hash |
| UNIT-06 / P0 — helper verification | Prepare PENDING, NONE, VERIFIED and missing users; call verification under the agreed actor contract | PENDING becomes VERIFIED and is saved; NONE/already VERIFIED/missing users produce agreed exceptions without a change. Pending-list operation returns only pending results using its intended repository query |

Do not compare a real BCrypt result with a fixed hash: it uses a salt. Actual
password matching, unique-email constraints, query filtering, transactions and
HTTP ADMIN access require integration tests. If admin enforcement lives in the
security chain, a direct service call cannot prove that enforcement.

## 5. Response mapping and contact privacy

### UNIT-07 — HelpRequestResponse mapper (P0)

Planned target: `HelpRequestResponse.from(HelpRequest request, User viewer,
Review review)` from SCRUM-22. Use real entities with distinct, non-null IDs and
populated contacts. Empty fixture contacts can conceal a broken mapper.

| Request state / viewer | Expected counterpart fields |
|---|---|
| OPEN, COMPLETED or CANCELLED / customer, helper, admin or unrelated viewer | Name, phone and email are all null |
| ACCEPTED / owning customer | Assigned helper's name and phone; email null |
| ACCEPTED / assigned helper | Customer's name, phone and email |
| ACCEPTED / admin, unrelated user or unassigned helper | All three null |

Also check ordinary request fields, category name, IDs/status/times, and review
fields both with a review and with null. This verifies the mapper's returned
values. The service must separately authorize reads and load the right review;
actual JSON output and hidden page markup have their own integration/manual checks.

## 6. Request and review services after implementation

Use the real service and mock its repositories. Actor identity comes from the
authenticated username supplied to the service, not a client-selected owner.
Service unit tests assert business outcomes/exceptions; HTTP codes belong in
controller/integration tests. Unsettled transitions remain pending a contract.

| ID / priority | Successful case | Rejections and other checks |
|---|---|---|
| UNIT-08 / P0 — create | Valid DTO, known customer/category → request saved with correct owner, fields, OPEN status and no helper; response hides contacts | Missing user/category follows the agreed rule; no request saved on rejection. Do not invent a missing-category exception before agreement |
| UNIT-09 / P0 — list own | Resolve current customer and return their mapped requests; empty result becomes an empty list | Select the current user's query; do not substitute another owner. Actual SQL filtering and newest-first ordering need PostgreSQL checks when delegated to the repository |
| UNIT-10 / P0 — details | An agreed permitted viewer receives the mapped request and applicable review | Unknown request rejected; unrelated viewer rejected. Confirm the final permitted-viewer contract; field privacy still follows UNIT-07 |
| UNIT-11 / P1 — edit | Owner edits their OPEN request; editable fields change while owner/status remain intact | Another customer and missing request rejected; no forbidden save. Confirm any remaining status restrictions |
| UNIT-12 / P1 — cancel | Owner cancels an OPEN request → CANCELLED; correct request saved | Another customer and missing request rejected. ACCEPTED cancellation and repeated cancellation remain unresolved |
| UNIT-13 / P0 — browse open | VERIFIED helper receives available request responses without contacts | Apply the final browsing eligibility rule. If the repository filters OPEN requests, verify real query behavior in integration coverage |
| UNIT-14 / P0 — accept | VERIFIED helper accepts OPEN request → ACCEPTED with that helper assigned | NONE/PENDING helper rejected; already-taken/missing request rejected; existing assignment is not replaced. Self-acceptance policy needs confirmation |
| UNIT-15 / P0 — complete | Assigned helper completes an ACCEPTED request → COMPLETED; assignment retained | Different helper, missing request and invalid state rejected without saving a forbidden transition |
| UNIT-16 / P1 — review DTO | Rating 1/5 accepted, 0/6 rejected; agreed comment length boundaries | Missing rating and optional comment need the DTO contract. Use 1000/1001 only if the DTO adopts that entity limit |
| UNIT-17 / P1 — review service | Owner reviews COMPLETED request; review saved for the correct request with submitted rating/comment | Wrong customer, missing request, pre-completion review and duplicate rejected under agreed rules; no duplicate save |

A database `@Column(nullable = false)` or length limit does not itself create
a Bean Validation rule. In particular, the current Review entity's `@Min` and
`@Max` do not establish a missing-rating requirement. Keep DTO rules, service
rules and database constraints at their owning test levels.

## 7. How to review the tests

- Use descriptive names such as `shouldRejectAcceptWhenHelperNotVerified`.
  State the actor and initial status so the reason for success/failure is clear.
- Check meaningful returned fields and the exact agreed exception type. Avoid
  treating "some exception occurred" as proof of the intended rejection.
- Mock dependencies, not the validator/mapper/service under test. A mock
  configured to return ACCEPTED does not prove acceptance logic.
- Check save intent when it proves a rule; avoid asserting every internal call
  or its order. Such assertions make harmless implementation changes break tests.
- Use fresh entities per case. `TestData` helps create them, but leaves IDs and
  phone fields unset; fill those when identity or contact privacy matters.
- Unit tests cannot prove persistence, rollback, uniqueness under concurrency,
  `@Version`, generated timestamps or the real security chain. Use the integration
  and E2E cases for those outcomes.
- Cover each agreed rule with success and applicable failures. No numerical
  line-coverage threshold or coverage plugin is configured by this checklist.

