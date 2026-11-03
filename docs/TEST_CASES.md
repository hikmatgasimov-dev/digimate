# Sprint 1 test cases — SCRUM-12

Status: draft for Hikmat's review. Test names below are a design checklist, not
implemented tests or a claim that the corresponding features already work.

## Sources and open questions

- SCRUM-12 and Hikmat's follow-up comment: PostgreSQL test profile, shared
  fixtures, one passing example and parameterised validator tests.
- `PRODUCT_BACKLOG.md`: MVP features and privacy rules; Jira remains authoritative.
- `PAGE CONTRACT.md`: exact customer-page routes, model attributes and rendering.
- `CODING_GUIDE.md`: layers, email as username and API error conventions.
- Current DTO constraints, entity mappings, security configuration and service
  signatures help identify cases, but do not replace acceptance criteria.

The card refers to Sprint 1 sections of four TODO files. They are absent from
this checkout. Before approving this list, attach or identify those files and
reconcile their Steps and Done when criteria with the cases below. Confirm the
REST routes, success status codes and service signatures for features that do
not yet exist. API cases marked "proposed" are not new endpoint contracts.

Also confirm: who may cancel an accepted request; allowed completion/cancellation
transitions; contact visibility after completion/cancellation; and whether an
unverified helper may browse requests or only needs verification to accept them.

## Test setup and existing examples

| Check | Test / evidence | State |
|---|---|---|
| Full Spring context loads with the test PostgreSQL profile | `DigimateApplicationTests.contextLoads` | Implemented |
| OPEN request fields and relationships survive a database round trip | `HelpRequestRepositoryTest.shouldPersistAndLoadOpenRequest` | Implemented |
| Normal text and null pass; ID/card patterns and length boundaries are checked | `NoSensitiveDataValidatorTest` parameterised cases | Implemented |
| DTO validation reports a sensitive description on the correct field | `HelpRequestDtoTest` | Already present |

Run `./mvnw test` with PostgreSQL running and `DB_USER`/`DB_PASSWORD` set. Use only
the disposable `digimate_test` database: its tables are created and dropped.
`@DataJpaTest` cases use `replace = NONE` and roll back after each test. Full
context tests must clean up any data they commit; `create-drop` does not reset
data between methods sharing the same Spring context.

## Customer-page endpoints (documented routes)

Successful pages normally return 200. Successful form submissions redirect to
`/requests/my` with `successMessage`. Validation errors return the form with
visible field errors; they are not API JSON 400 responses. Unauthenticated page
requests redirect to login. Form POSTs without a valid CSRF token return 403.

| Endpoint | Success / input test names | Access / failure test names |
|---|---|---|
| `GET /requests/new` | `shouldShowEmptyRequestFormWithCategories` | `shouldRedirectAnonymousUserToLogin` |
| `POST /requests` | `shouldCreateRequestAndRedirectWithSuccessMessage`; `shouldRedisplayInvalidFormWithFieldErrorsAndCategories`; `shouldBindDatetimeLocalPreferredTime` | `shouldRejectCreateWithoutCsrf`; `shouldRedirectAnonymousCreateWithCsrfToLogin` |
| `GET /requests/my` | `shouldShowOnlyCurrentUsersRequestsNewestFirst`; `shouldShowEmptyRequestsMessage` | `shouldRedirectAnonymousMyRequestsToLogin` |
| `GET /requests/{id}` (later card) | `shouldShowRequestVisibleToCurrentUser` | `shouldDenyUnrelatedUserRequestDetails`; `shouldReturn404ForMissingRequest`; `shouldRedirectAnonymousDetailsToLogin` |
| `GET /requests/{id}/edit` | `shouldShowOpenRequestFormWithEditIdAndCategories` | `shouldDenyEditingAnotherCustomersRequest`; `shouldReturn404ForMissingEditRequest`; `shouldRedirectAnonymousEditToLogin` |
| `POST /requests/{id}` | `shouldUpdateRequestAndRedirectWithSuccessMessage`; `shouldRedisplayInvalidEditWithEditIdAndCategories` | `shouldRejectUpdateWithoutCsrf`; `shouldDenyUpdatingAnotherCustomersRequest`; `shouldReturn404ForMissingUpdateRequest`; `shouldRedirectAnonymousUpdateWithCsrfToLogin` |
| `POST /requests/{id}/cancel` | `shouldCancelRequestAndRedirectWithSuccessMessage` | `shouldRejectCancelWithoutCsrf`; `shouldDenyCancellingAnotherCustomersRequest`; `shouldReturn404ForMissingCancelRequest`; `shouldRedirectAnonymousCancelWithCsrfToLogin` |

Confirm page handling for forbidden actions and invalid status transitions before
asserting an exact error view or HTTP status. The API exception handler is scoped
to `com.digimate.controller.api` and does not establish the page error contract.

## Proposed REST endpoint cases (routes await acceptance criteria)

For an API that exposes these operations, test the relevant rows below. Do not
require every status for every endpoint. Use real security filters when checking
401/403; supplying a mock principal alone does not exercise HTTP Basic login.

| Operation | Proposed test names and applicable outcomes |
|---|---|
| Registration (`POST /api/auth/register` is reserved by the security config) | `shouldRegisterCustomer`; `shouldRegisterPendingHelperWhenRequested`; `shouldReturn400ForInvalidRegistration`; `shouldReturn409ForDuplicateEmail`; `shouldAllowRegistrationWithoutAuthentication` |
| Login / authentication | `shouldAuthenticateWithEmailAndPassword`; `shouldReturn401ForWrongApiPassword`; `shouldReturn401ForAnonymousProtectedApiRequest`; page login success/failure follows its separate form contract |
| Admin helper verification | `shouldVerifyPendingHelperAsAdmin`; `shouldReturn401ForAnonymousVerification`; `shouldReturn403ForNonAdminVerification`; missing-user behavior and invalid transitions await confirmation |
| Create request | `shouldCreateOpenRequestForAuthenticatedCustomer`; `shouldReturn400ForInvalidRequest`; `shouldReturn401ForAnonymousCreate`; unknown-category behavior awaits confirmation |
| List own requests | `shouldReturnOnlyCurrentCustomersRequestsNewestFirst`; `shouldReturnEmptyListWhenCustomerHasNoRequests`; `shouldReturn401ForAnonymousList` |
| Request details | `shouldReturnVisibleRequest`; `shouldReturn401ForAnonymousDetails`; `shouldReturn403ForUnrelatedUser`; `shouldReturn404ForUnknownRequest` |
| Edit request | `shouldUpdateOwnOpenRequest`; `shouldReturn400ForInvalidUpdate`; `shouldReturn401ForAnonymousUpdate`; `shouldReturn403ForAnotherCustomersUpdate`; `shouldReturn404ForUnknownUpdateRequest`; invalid-state conflict awaits confirmation |
| Cancel request | `shouldCancelOwnRequest`; `shouldReturn401ForAnonymousCancel`; `shouldReturn403ForAnotherCustomersCancel`; `shouldReturn404ForUnknownCancelRequest`; repeated/invalid-state conflict awaits confirmation |
| Browse open requests | `shouldListOpenRequestsForVerifiedHelper`; `shouldReturn401ForAnonymousBrowse`; eligibility rules for other users await confirmation |
| Accept request | `shouldAcceptOpenRequestAsVerifiedHelper`; `shouldReturn401ForAnonymousAccept`; `shouldReturn403ForUnverifiedHelper`; `shouldReturn404ForUnknownAcceptRequest`; `shouldReturn409WhenRequestAlreadyAccepted` |
| Complete request | `shouldCompleteAcceptedRequestAsAssignedHelper`; `shouldReturn401ForAnonymousComplete`; `shouldReturn403ForAnotherHelpersCompletion`; `shouldReturn404ForUnknownCompleteRequest`; `shouldReturn409ForInvalidCompletionTransition` |
| Review completed request | `shouldReviewCompletedRequestAsItsCustomer`; `shouldReturn400ForRatingOutsideOneToFive`; `shouldReturn401ForAnonymousReview`; `shouldReturn403ForAnotherCustomersReview`; `shouldReturn404ForUnknownReviewedRequest`; `shouldReturn409ForDuplicateReview`; pre-completion behavior awaits confirmation |

For API errors, check both status and the agreed `ErrorResponse` shape. Validate
malformed JSON, invalid enum/date values and invalid ID types where those inputs
are accepted. Verify these cases through controller tests when handlers exist.

## Service unit-test design

Service tests check business results and rejected operations independently of
HTTP. Mock repositories at this layer; use real DTOs, entities and mapping logic.

| Service area | Proposed test names |
|---|---|
| `create(dto, username)` | `shouldCreateOpenRequestOwnedByCurrentUser`; `shouldUseSelectedCategory`; `shouldHideContactsForOpenRequest`; unknown user/category handling awaits confirmation |
| `findMine(username)` | `shouldReturnOnlyOwnedRequestsNewestFirst`; `shouldReturnEmptyListForCustomerWithoutRequests` |
| `findById(id, username)` | `shouldReturnRequestForAuthorizedViewer`; `shouldRejectUnrelatedViewer`; `shouldRejectUnknownRequest`; `shouldExposeContactsOnlyToParticipantsAfterAcceptance` |
| `update(id, dto, username)` | `shouldUpdateEditableFieldsOfOwnOpenRequest`; `shouldPreserveOwnerAndStatusOnUpdate`; `shouldRejectUpdateByAnotherCustomer`; `shouldRejectUnknownUpdateRequest`; allowed statuses await confirmation |
| `cancel(id, username)` | `shouldCancelOwnCancellableRequest`; `shouldRejectCancellationByAnotherCustomer`; `shouldRejectUnknownCancellationRequest`; allowed/repeated transitions await confirmation |
| Registration (signature pending) | `shouldEncodePasswordBeforeSaving`; `shouldRejectDuplicateEmail`; `shouldCreateCustomerWithoutHelperPrivileges`; `shouldCreatePendingHelperWhenRequested` |
| Helper verification (signature pending) | `shouldVerifyPendingHelper`; `shouldRejectVerificationByNonAdmin`; missing helper and other transitions await confirmation |
| Acceptance (signature pending) | `shouldAssignVerifiedHelperAndAcceptOpenRequest`; `shouldRejectPendingHelperAcceptance`; `shouldRejectNonHelperAcceptance`; `shouldRejectAlreadyAcceptedRequest`; self-acceptance policy awaits confirmation |
| Completion (signature pending) | `shouldCompleteRequestAsAssignedHelper`; `shouldRejectCompletionByAnotherHelper`; `shouldRejectInvalidCompletionTransition` |
| Review (signature pending) | `shouldSaveReviewForCompletedRequestOwner`; `shouldRejectReviewByAnotherCustomer`; `shouldRejectDuplicateReview`; service-level rating and pre-completion checks await confirmation |

Add one database/concurrency test for two helpers accepting the same request when
the acceptance implementation exists: exactly one assignment must win, and the
other attempt must receive the agreed conflict outcome. A mocked repository
cannot establish the optimistic-locking behavior of `HelpRequest.version`.

## DTO and page boundaries

- `title`: null, blank, 100 characters, 101 characters.
- `description`: null, blank, 1000 characters, 1001 characters and sensitive data.
- `categoryId` and `mode`: required; distinguish a missing ID from a nonexistent
  category, and a missing enum from an unparseable enum.
- `preferredTime`: missing, past and future; browser `datetime-local` binding.
- `place`: absent, 255 characters and 256 characters.
- Request pages: correct `helpRequest`, `categories`, `editId`, `requests` and
  flash-message attributes; retain submitted values after validation failure.
- OPEN/ACCEPTED/COMPLETED/CANCELLED rendering follows `PAGE CONTRACT.md`; display
  contacts only when the service supplies them, and offer a review only when
  completed and not already reviewed.
- Do not expose another customer's requests or participant contact data in list,
  detail, error or log output. Use synthetic fixture data throughout.

## Review completion

Hikmat must review the proposed scenarios against the missing TODO files and
current Jira acceptance criteria. Mykolas should review the README setup text.
Review, merge and Jira status changes are team workflow steps; this draft does
not record them as completed.
