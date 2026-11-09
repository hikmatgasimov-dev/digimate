# DigiMate test plan and cases — SCRUM-12

Updated: **3 November 2026**. Target: **Wednesday, 11 November 2026**.
Status: proposed coverage and work plan for Hikmat's review. Only the tests in
"What exists today" are implemented. The cases below have not been executed.

## 1. What we are testing

The goal is to check that people can request and provide help, with correct
permissions, saved data and private contact details.

| Test type | What it checks | Example |
|---|---|---|
| E2E through the API | A complete business journey through the running backend and PostgreSQL | Register → verify helper → create → accept → complete |
| Integration | A focused collaboration between real parts of the monolith | Registration saves a BCrypt password that the security chain can use for login |
| Manual frontend | A person uses the running site and checks forms, navigation and displayed results | Submit an invalid form, correct it, then find the saved request |
| Unit / DTO validation | One rule or boundary, quickly and precisely | Title: 100 characters accepted, 101 rejected |

Frontend checks are manual. Backend E2E uses HTTP requests;
its scope ends at the backend response and persisted data, so it cannot prove
that a page renders or a button works.

E2E and integration can use similar Spring test infrastructure in a monolith.
Here the distinction is their purpose: E2E follows a whole journey; integration
isolates a collaboration or failure. Avoid repeating every boundary in both.

**P0** means the core journey, permissions, privacy or data integrity.
**P1** means the remaining MVP coverage, including editing, cancellation and
reviews. P1 still belongs in the release plan; a gap must be reported.

## 2. What exists today

Checked in the current local checkout on 3 November. Other branches may differ.

| Existing item | Coverage / limitation |
|---|---|
| `NoSensitiveDataValidatorTest` | Parameterized accepted/rejected descriptions, including sensitive-number boundaries |
| `HelpRequestDtoTest` | Normal description accepted; synthetic card/ID examples rejected |
| `HelpRequestRepositoryTest` | A real PostgreSQL save/reload with customer, category, OPEN status, timestamps and version |
| `DigimateApplicationTests` | Application-context startup; no business journey |
| Test profile and `TestData` | Disposable PostgreSQL configuration and unsaved fixture factories |
| Application features | No business controllers locally; the five `HelpRequestService` methods are placeholders; customer templates contain sample data |

The existing command is `./mvnw test`; setup is in [README.md](../README.md).
It runs the implemented tests, not the planned E2E/manual cases. When automated
cases are added, confirm Maven discovers them and document their run command.
Do not assume classes named `*IT` run: no Failsafe execution is configured today.

## 3. Work plan to 11 November

The agreed working scope is API E2E, integration and manual frontend checks,
using the existing Java/Spring test stack. No new test dependencies are assumed.
The following blocks reserve time for implementing tests, understanding results,
debugging and retesting; feature delivery is an external dependency.

| Day / reserved time | Main work | Expected output |
|---|---|---|
| Before Friday, 6 Nov — preparation when available | Confirm API/page readiness and open rules; review existing tests and prepare case data | Known dependencies; no promised implementation hours before Friday |
| Friday, 6 Nov — 2–3 h | Run the existing suite; prepare isolated data and HTTP authentication; extend the most useful DTO boundaries | Reproducible baseline and a ready test setup; first runnable registration/integration checks if the API exists |
| Saturday, 7 Nov — 6 h | Build E2E-01 in small steps; add E2E-02/03 and registration/security integration checks | Core API journey and critical rejection paths, or precise blockers for missing endpoints |
| Sunday, 8 Nov — 6 h | Check privacy, transactions and concurrent acceptance; manually test available pages | Focused P0 integration coverage, first manual results and reproducible defects |
| Monday, 9 Nov — 4–5 h | Cover edit/cancel/review; complete manual checks as pages arrive; retest fixes | Remaining MVP coverage and an explicit list of unresolved gaps |
| Tuesday, 10 Nov — 4–5 h | Run the full applicable suite, API journeys and manual regression; leave time for fixes and reruns | Results for the final tested revision; core flows checked before Wednesday |
| Wednesday, 11 Nov — up to 4–5 h available | Recheck fixes on the final revision, review evidence and report remaining risks | Final test report; after freeze, verify the frozen build without planning new changes |

**Readiness gate:** check the lifecycle endpoints by Friday. If they are absent,
mark dependent E2E cases Blocked and continue DTO, persistence and available
integration work. On Sunday, reassess delivery with the team if P0 is still
blocked. Missing frontend pages similarly block their manual cases. Extra test
hours cannot replace missing application features.

## 4. Shared test data and execution rules

- Use a disposable PostgreSQL database and fictional data only. Prepare two
  customers, a PENDING helper, two VERIFIED helpers, an admin and a category.
  Use populated names/phones/emails when checking contact privacy.
- Create the admin as a test fixture; public registration must never create
  ADMIN. Give every scenario unique emails and its own requests. Generate
  future appointment times relative to the run date.
- E2E calls a running application over real HTTP, with the real security chain,
  controllers, services, repositories and database. Do not mock business layers.
  Use the existing JUnit/Spring facilities and a Java HTTP client.
  Here, an HTTP client is the requester used by the test: it sends a URL, method,
  headers and optional JSON, then returns the server's status, headers and body.
  Java 21 includes [java.net.http.HttpClient](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html),
  so this choice needs no additional client dependency. JUnit runs the test and
  assertions; the client sends requests; the application handles them. Spring's
  test setup can start the local server as part of the test lifecycle.
- Use each actor's own HTTP Basic credentials on API requests. Test missing and
  wrong credentials as well. A mock principal does not prove authentication.
- Fixtures may establish preconditions; actions under test go through the
  scenario's interface. If registration is being tested, register through HTTP.
- Verify saved outcomes with a fresh read. Repository tests flush and clear the
  persistence context; rollback/commit checks read in a separate transaction.
  Failed actions must leave ownership, assignment, status and row counts intact.
- The test profile uses `create-drop`. Do not run an application and a test suite
  against the same database concurrently. HTTP E2E writes commit on server
  threads; a test method's rollback does not clean them up. Reset only data in
  the isolated test database between scenarios.
- Record each case as Planned, Ready, Blocked, Passed or Failed. Blocked means a
  prerequisite or agreed rule is missing; Failed means an executed check differs
  from an agreed expectation. None of the planned cases is Passed yet.

## 5. E2E scenarios through the API

All cases are planned. Exact request-lifecycle paths and some response codes
remain proposals in [ENDPOINTS.md](ENDPOINTS.md); confirm them before automation.
These journeys use independent data and can run in any order.

### E2E-01 — Request from registration to completion (P0)

Prerequisites: registration, verification and request-lifecycle endpoints exist;
a fixture admin and category are available.

1. Register a customer and a helper who wants to help. Expect 201, role USER,
   and helper states NONE / PENDING respectively. Responses expose no password.
2. Authenticate as admin, find the PENDING helper and verify them. Expect 200;
   the helper is VERIFIED and disappears from the pending list.
3. As the customer, create a valid request. Expect the agreed creation response
   (proposed 201), an ID, OPEN status and no assigned helper. Find the same ID in
   the customer's list and the VERIFIED helper's open list; contacts are hidden.
4. As that helper, accept it. Expect ACCEPTED and the correct assignment after
   a fresh read; it leaves the open list. Check participant contacts using the
   privacy matrix below.
5. As the assigned helper, complete it. Expect COMPLETED after fresh reads by
   both participants, retained assignment and hidden contacts again.

This case proves the functions work together. Reviews are a separate journey
so a missing review endpoint does not prevent checking the core lifecycle.

| ID / priority | Actions | Expected result |
|---|---|---|
| E2E-02 / P0 | A PENDING helper tries to accept an OPEN request; admin verifies them; the helper retries | First attempt: 403 with unchanged request. After verification: acceptance succeeds and the saved assignment is correct |
| E2E-03 / P0 | Helper A accepts; helper B then tries the same request; A completes it | B receives 409 and cannot replace A; completion by A succeeds. This is a sequential conflict check; the simultaneous race is INT-07 |
| E2E-04 / P1 | Customer creates an OPEN request, edits it, then cancels it | Fresh reads show edited values with the same owner; final status is CANCELLED and the request is absent from the helper's open list |
| E2E-05 / P1 | The assigned helper completes a request; its customer submits a valid review, reads it and submits again | First review is saved and returned; duplicate is rejected with 409 and one review remains. Use prepared COMPLETED data if lifecycle behavior is already covered by E2E-01 |

## 6. Integration checks for the monolith

Use real collaborating components for the boundary named below. Full web checks
can use a running server or full-context MockMvc with real services and
PostgreSQL. A controller test with mocked services has narrower coverage.
Only HTTP E2E checks are counted as the backend journeys in section 5.

| ID / priority | Components and check | Expected result |
|---|---|---|
| INT-01 / P0 — persistence | Repositories + entity mappings + PostgreSQL: save and reload a request | Correct customer/category, OPEN state, empty helper, timestamps and version; an example already exists |
| INT-02 / P0 — registration and login | Registration + database + BCrypt + security: register, then authenticate with email/password; try a duplicate with changed email case | Normalized email; stored hash matches the password; real authentication succeeds; duplicate returns 409 with no extra user; USER and NONE/PENDING privileges only |
| INT-03 / P0 — access and HTTP errors | Real filters + controllers + exception handling: anonymous/wrong password; non-admin at admin routes; another customer's list/detail/edit/cancel; unknown IDs | Applicable 401/403/404 outcomes; lists contain only owned requests; no forbidden changes. API handler errors use `timestamp`, `status`, `message`, `path`; confirm filter error bodies separately |
| INT-04 / P0 — verification | Admin operation + user query + transaction: list pending, verify, reread; repeat and use NONE/unknown users | Only PENDING in list, including an empty result; PENDING → VERIFIED persists; unknown user 404, NONE/already VERIFIED 409; failures leave state unchanged |
| INT-05 / P0 — response privacy | Service + request/review repositories + response mapper: read lists/details for each allowed viewer and state | Exact privacy matrix below; review fields mapped when present and null when absent; unrelated detail access denied under the agreed read rule |
| INT-06 / P0 — acceptance and completion | Request service + authorization + transaction + database: accept, complete; try an unverified or different helper and invalid transitions | Correct persisted assignment/status; forbidden actors rejected; agreed state conflicts; rejected actions leave data unchanged |
| INT-07 / P0 — concurrent acceptance | Two VERIFIED helpers accept one OPEN request in independent concurrent transactions | Exactly one succeeds, the loser gets the agreed conflict (API 409), and a fresh read shows one retained assignment. Use committed setup and controlled synchronization; a mocked repository or sequential calls cannot prove this |
| INT-08 / P0 — validation and atomicity | JSON/DTO validation + service + database: invalid body, sensitive description, malformed JSON/date/enum/ID; rejected state changes | Applicable 400/error outcomes; no created request or partial updates after rejection. Confirm unknown-category status separately |
| INT-09 / P1 — edit and cancel | Service + ownership rules + persistence + open-list query | Only the owner changes the allowed request; updates preserve owner/status; OPEN cancellation persists and removes it from the open list; accepted cancellation is unresolved |
| INT-10 / P1 — review | Review service + request state + unique request/review constraint | Owning customer only, COMPLETED only, rating 1–5, one review per request; failures leave stored review/state unchanged; confirm unsettled error codes |

### Contact privacy — SCRUM-22

Use participants with non-empty contact fields so accidental empty fixtures
cannot hide a broken mapping. Check list and detail responses where permitted.

| State / viewer | Allowed counterpart fields |
|---|---|
| OPEN, COMPLETED or CANCELLED / anyone | None: name, phone and email are null |
| ACCEPTED / owning customer | Helper name and phone; email is null |
| ACCEPTED / assigned helper | Customer name, phone and email |
| ACCEPTED / admin, unrelated user or unassigned helper | None |

Read permission and field privacy are separate: an admin permitted to read
still receives no contacts; an unrelated user may be denied the whole response.
The planned pure mapper target is
`HelpRequestResponse.from(HelpRequest request, User viewer, Review review)`;
`review` may be null. Cover all matrix combinations cheaply in unit tests and
check their use by real services in INT-05.

## 7. Manual frontend checklist

Execute these by hand in the running application once the relevant pages exist.
Static template previews can check layout, but cannot verify saved data or
permissions. Use separate browser profiles/sessions for customer, helper and
admin. Confirm the target desktop browser before the final run; browser
compatibility expansion and mobile layout are not assumed in this time budget.

| ID / priority | What to do | Expected visible result |
|---|---|---|
| MAN-01 / P0 — account access | Register with and without helper preference; log in with correct/wrong password; log out and reopen a protected page | Clear registration/login feedback; correct account access; after logout the protected page redirects to login |
| MAN-02 / P0 — create | Open New request, check privacy warning, fill each field and submit; cover ONLINE and IN_PERSON | Warning before submission; categories load; success feedback; one matching OPEN request in My requests after reload |
| MAN-03 / P0 — errors | Submit missing/invalid values and a synthetic sensitive number, then correct the form | Visible errors beside fields; valid input retained; no request created on rejection; correction creates one request |
| MAN-04 / P0 — lists and ownership | Check an empty account, multiple own requests and another customer's direct detail/edit URL | Useful empty state; own requests newest first; correct values/statuses after reload; forbidden access discloses no data |
| MAN-05 / P0 — helper journey | As VERIFIED helper browse, accept and complete; try PENDING access and a second helper's stale acceptance | Correct status/assignment feedback; pending acceptance denied; already-taken feedback without overwriting the first helper. Browsing restriction awaits confirmation |
| MAN-06 / P0 — admin verification | Open pending helpers as admin, verify one, then retry helper actions; try the admin URL as a non-admin | Updated pending list and helper eligibility; non-admin denied, including direct URL access |
| MAN-07 / P0 — contacts | Inspect populated contacts in OPEN, ACCEPTED and COMPLETED/CANCELLED as the relevant actors | Exact matrix above; disallowed contact values are absent from hidden markup as well as the visible page |
| MAN-08 / P1 — edit/cancel | Edit an own OPEN request, trigger an edit error, then cancel it | Prefilled form; errors retain values; saved changes persist; cancellation shows CANCELLED. Do not guess ACCEPTED cancellation behavior |
| MAN-09 / P1 — review | Review own completed request and attempt another review | Rating/comment displayed after reload; unavailable before completion; duplicate cannot create another review |
| MAN-10 / P1 — page usability | Navigate links/forms with keyboard; inspect labels, headings, field errors and displayed times | Working navigation; labels linked to inputs; `lang="en"`, one h1; usable errors; preferred time matches input and UTC creation time uses the agreed app time zone |

Successful writes redirect to `/requests/my` with `successMessage`; invalid forms redisplay errors. Check correct form/model
bindings (`helpRequest`, `categories`, `editId`, `requests`) in page integration
checks. Form POSTs need CSRF: missing/invalid token returns 403; anonymous GETs
redirect to login. With a valid token, anonymous protected POSTs require login.
Exact forbidden/not-found/conflict page views remain open; the API exception
handler does not define them.

## 8. DTO boundaries and focused unit checks

See [UNIT_TESTS.md](UNIT_TESTS.md) for the separate unit-test checklist, including
service rules, mapping, fixtures, expected failures and current implementation gaps.

Keep most combinations here, then check representative invalid input through
HTTP and manually. Start with the existing tests and add missing coverage;
avoid duplicating feature owners' tests.

| Input | Valid / invalid boundaries to check |
|---|---|
| Required text | Null, empty and whitespace-only title, description and registration text fields are rejected |
| Request title | 100 characters accepted; 101 rejected |
| Request description | 1000 accepted; 1001 rejected; ordinary text accepted; synthetic ID/card-like strings rejected |
| Sensitive-number validator | Preserve existing checks: 12 digits accepted; 13 and 19 rejected; spaced/hyphenated card formats and the ID pattern rejected; null is left to the required-field validator |
| Category and mode | Missing values rejected; ONLINE and IN_PERSON accepted; distinguish nonexistent category IDs from missing values and unknown enum text |
| Preferred time | Missing/past rejected; future accepted; verify `datetime-local` binding when exercising the page |
| Place | Optional; 255 characters accepted; 256 rejected. No extra mandatory-place rule is agreed |
| Registration fullName | 100 accepted; 101 rejected |
| Registration email | Valid format; 255/256 length boundary; normalization and case-insensitive duplication checked in INT-02 |
| Registration phone | Required; 30/31 length boundary; no invented format rule |
| Registration password | 7 rejected, 8 accepted; 72/73 character boundary per SCRUM-24; Unicode byte-limit behavior needs agreement because BCrypt limits bytes |
| Review rating/comment | Ratings 1 and 5 valid, 0 and 6 invalid; missing-rating behavior needs the DTO contract. Comment 1000/1001 if the review DTO adopts the entity limit |

Registration input is `fullName`, `email`, `phone`, `password`, `wantsToHelp`.
`UserResponse` exposes only `id`, `fullName`, `email`, `role`, `helperStatus`;
password and hash must be absent from success/error responses. Public input must
not grant ADMIN or VERIFIED. Service unit tests also cover ownership, helper
eligibility, allowed transitions and review rules without involving HTTP.

## 9. What counts as finished

- Applicable mandatory checks pass for the final revision. The coding guide
  requires `./mvnw test` before a PR; the PR template also requires starting the
  app and trying it in Swagger or the browser. Run and record the added API
  E2E command as well if it is separate. Local results do not establish CI status.
- Record case ID, tested revision/environment, expected/actual result and status.
  Include repro steps and useful screenshots/logs for defects; redact credentials
  and session tokens. Application logs contain IDs only, no contact/request text.
- P0 cases have execution results; P1 MVP cases also have results or explicitly
  reported release gaps. Unresolved Blocked/Failed cases remain visible with the
  missing dependency or defect and the next step; they do not count as Passed.
- After a fix, rerun the affected case and relevant regression on the final
  inputs. A green startup check, Swagger page or DTO test alone does not prove
  the request lifecycle or the frontend works.
