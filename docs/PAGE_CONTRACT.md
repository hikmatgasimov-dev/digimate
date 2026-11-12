# Page contract: browser pages

The names below are fixed. Controllers put exactly these model attributes; templates read exactly these names.
This file describes what is built and live. When you change a page, change this file in the same pull request.

Controllers: `src/main/java/com/digimate/controller/web/`
Templates: `src/main/resources/templates/`

All pages use the shared layout (`fragments/layout :: head(...)` and `:: nav`).
The request cards are shared fragments in `fragments/request-parts.html`.
The plain `<link>` and `href` inside `th:` elements are only for opening a file directly; Thymeleaf replaces them.
All forms use `th:action`, so Spring adds the CSRF token.
`username` below means the logged-in email (`authentication.getName()`).

## Routes

Public routes: `/login`, `/register`, `/error`, `/css/**`, `/webjars/**`, Swagger. Every other route needs a login.
Routes under `/admin/**` need the ADMIN role.

| Method | URL | Controller | Result |
|---|---|---|---|
| GET  | `/` | AuthPageController | admin: redirect `/admin/helpers`; everyone else: redirect `/requests/my` |
| GET  | `/login` | AuthPageController | `login` |
| POST | `/login` | Spring Security | success: `/`; wrong login: `/login?error` |
| POST | `/logout` | Spring Security | redirect `/login?logout` |
| GET  | `/register` | AuthPageController | `register` with an empty form |
| POST | `/register` | AuthPageController | valid: redirect `/login?registered`; invalid: `register` again with errors |
| GET  | `/requests/new` | RequestPageController | `request-new` with an empty form |
| POST | `/requests/new` | RequestPageController | valid: redirect `/requests/my` with `successMessage`; invalid: `request-new` again with errors |
| GET  | `/requests/my` | RequestPageController | `requests-my` |
| GET  | `/requests/{id}/edit` | RequestPageController | `request-new` filled in; form posts to `/requests/{id}/edit` |
| POST | `/requests/{id}/edit` | RequestPageController | redirect `/requests/my` with `successMessage` |
| POST | `/requests/{id}/cancel` | RequestPageController | redirect `/requests/my` with a message |
| POST | `/requests/{id}/review` | RequestPageController | redirect `/requests/my` with a message |
| POST | `/requests/{id}/complaint` | RequestPageController | redirect `/requests/my` with a message |
| GET  | `/helper` | HelperPageController | `helper-inbox` |
| POST | `/helper/requests/{id}/accept` | HelperPageController | redirect `/helper` with a message |
| POST | `/helper/requests/{id}/complete` | HelperPageController | redirect `/helper` with a message |
| GET  | `/admin/helpers` | AdminPageController | `admin-helpers` |
| GET  | `/admin/users` | AdminPageController | `admin-users` |
| POST | `/admin/helpers/{id}/verify` | AdminPageController | redirect `/admin/helpers` with a message |
| POST | `/admin/helpers/{id}/remove` | AdminPageController | redirect `/admin/helpers` with a message |
| POST | `/admin/complaints/{id}/dismiss` | AdminPageController | redirect `/admin/helpers` with a message |

There is no detail page for one request. Everything about a request is on its card in `requests-my` or `helper-inbox`.

Error pages (templates in `templates/error/`): `403` "Access denied", `404` "Page not found", `500` "Something went wrong".

## Messages after a redirect (flash attributes)

Every page that can be the target of a redirect shows these two names:

- `successMessage` in `<output class="alert alert-success">`
- `errorMessage` in `<div class="alert alert-danger" role="alert">`

Both are `String` or absent.

## login.html

- Form action: `@{/login}`, method POST.
- Email input: `name="username"`. Password input: `name="password"`.
- `param.error`: wrong login, class `error`.
- `param.logout`: logged out, class `success`.
- `param.registered`: registration done, class `success`.

## register.html

Model attribute: `form` (`RegisterRequest`). Form action `@{/register}`, method POST.

| Field | Input | Rules |
|---|---|---|
| `fullName` | text | required, max 100 |
| `email` | email | required, valid email, max 255, must not be used yet |
| `phone` | text | required, max 30 |
| `password` | password | required, 8 to 72 characters |
| `wantsToHelp` | checkbox, unchecked by default | optional; when checked the user becomes a helper waiting for approval |

- Errors: `<p class="field-error" th:errors="*{field}">` under each field.
- A used email is an error on the field `email`.
- Invalid input shows `register` again.

## request-new.html (create and edit)

Admins cannot create or change requests. They are redirected to `/requests/my` with `errorMessage`.

Model attributes:

| Name | Type | Notes |
|---|---|---|
| `form` | `HelpRequestDto` | `th:object="${form}"`; on errors the `BindingResult` follows it |
| `categories` | `List<Category>` | sorted by name; use `id` and `name` for the select |
| `formTitle` | `String` | create: "Ask for help"; edit: "Edit request". Used in the `<h1>` |
| `formAction` | `String` | create: `/requests/new`; edit: `/requests/{id}/edit`. Used in `th:action` |
| `submitLabel` | `String` | create: "Send request"; edit: "Save changes" |

Form fields (`name` = DTO field):

| Field | Input | Rules (from `HelpRequestDto`) |
|---|---|---|
| `title` | text | required, max 100, no sensitive data |
| `description` | textarea | required, max 1000, no sensitive data |
| `categoryId` | select, options from `categories` | required |
| `preferredTime` | `datetime-local`, format `yyyy-MM-dd'T'HH:mm` | required, in the future |
| `mode` | two radios, `IN_PERSON` and `ONLINE` | required |
| `place` | text | max 255, no sensitive data; required when `mode` is `IN_PERSON` (checked in the controller, on create and on update) |

- "No sensitive data" means the `@NoSensitiveData` check: card and ID numbers are refused.
- Errors: one `<p class="field-error" th:errors="*{field}">` under each field, and `aria-invalid="true"` on the invalid input.
- Edit only works while the request is `OPEN`. Other statuses redirect to `/requests/my` with `errorMessage`.

## requests-my.html (customer cards)

Model attributes:

| Name | Type | Notes |
|---|---|---|
| `requests` | `List<HelpRequestResponse>` | the customer's own requests; empty list shows the "no requests" text |
| `complaintStatuses` | `Map<Long, ComplaintStatus>` | key is the request id; a key means a report already exists for that request |
| `successMessage` | `String` or absent | flash |
| `errorMessage` | `String` or absent | flash |

Fields used per item (`HelpRequestResponse`): `id`, `title`, `description`, `categoryName`, `mode`, `place`, `preferredTime`,
`createdAt`, `status`, `counterpartName`, `counterpartPhone`, `counterpartEmail`, `reviewRating`, `reviewComment`.

The card is the fragment `customer-card(request, complaintStatuses)`. What each status shows:

| Status | Shown | Actions |
|---|---|---|
| `OPEN` | status badge, request details | Edit (link to `/requests/{id}/edit`), Cancel request (POST) |
| `ACCEPTED` | helper name, phone, email in a contact box | Report a problem form |
| `COMPLETED` | helper contact box, rating and comment if reviewed | Rate form when `reviewRating` is null; Report a problem form |
| `CANCELLED` | status badge, request details | none |

- Cancel and edit work only while the request is `OPEN`.
- Show the contact box only when `counterpartName` is not null. The service decides, not the page.
- `createdAt` is an `Instant` (UTC). `preferredTime` is a `LocalDateTime`.
- Status badge: `class="status status-open"` (also `status-accepted`, `status-completed`, `status-cancelled`).

### Review form (COMPLETED, `reviewRating` is null)

| Item | Value |
|---|---|
| Action | `POST /requests/{id}/review` |
| `rating` | `select`, values 1 to 5, `id="rating-{id}"`, required (`ReviewDto`: `@NotNull @Min(1) @Max(5)`) |
| `comment` | `textarea`, `id="comment-{id}"`, optional, max 1000, no sensitive data |

Results (flash, redirect `/requests/my`): saved; already reviewed; request not completed; not your request; request not found.

### Report a problem form (ACCEPTED or COMPLETED, no report yet)

| Item | Value |
|---|---|
| Action | `POST /requests/{id}/complaint` |
| `reason` | `textarea`, `id="reason-{id}"`, required, max 500, no sensitive data |

- One report per request. After it is sent, the card shows "You reported a problem. An admin will look at it."
- Only the customer of the request can report, and only after a helper accepted.

## helper-inbox.html

Only verified helpers get the lists. A helper who is not verified sees only the `errorMessage` sentence.

| Model attribute | Type | Notes |
|---|---|---|
| `openRequests` | `List<HelpRequestResponse>` | `OPEN` requests; contact fields are always null. Absent for a helper who is not verified |
| `myRequests` | `List<HelpRequestResponse>` | requests this helper accepted (`ACCEPTED` and `COMPLETED`) |
| `successMessage` | `String` or absent | flash |
| `errorMessage` | `String` or absent | flash, or the "not verified" sentence |

| Section | Fragment | Shown | Action |
|---|---|---|---|
| Open requests | `open-card` | title, description, category, mode, place, preferred time (no customer name) | Accept (POST `/helper/requests/{id}/accept`) |
| My requests | `helping-card` | status badge, contact box with the customer | Mark as completed (POST `/helper/requests/{id}/complete`), only while `ACCEPTED` |

- Accept checks the status again (`OPEN` only). If another helper was faster, the page redirects to `/helper` with
  "This request was just accepted by another helper. Please choose another one."
- A helper cannot accept their own request.

## admin-helpers.html

Model attributes:

| Name | Type | Notes |
|---|---|---|
| `pending` | list of `UserResponse` | helpers waiting for approval |
| `reports` | list of `ComplaintResponse` | open customer reports |
| `verified` | list of `UserResponse` | verified helpers |
| `successMessage` | `String` or absent | flash |
| `errorMessage` | `String` or absent | flash |

Sections, in this order:

1. **Waiting for approval**: each helper has `id`, `fullName`, `email`. Button Verify: `POST /admin/helpers/{id}/verify`.
2. **Reports from customers**: each report has `id`, `requestId`, `helperId`, `helperName`, `helperEmail`, `customerFirstName`, `text`.
   Phone numbers are never shown. Two actions:
    - Dismiss: `POST /admin/complaints/{id}/dismiss`, optional field `note` (max 500, no sensitive data).
    - Remove helper: `POST /admin/helpers/{helperId}/remove`.
3. **Verified helpers**: a table with Remove helper next to each helper.

Remove helper form: field `reason`, `textarea`, required, max 500, no sensitive data (`RemoveHelperDto`).
After a removal, the helper's accepted requests become `OPEN` again.

## admin-users.html

Model attributes: `users` (list of `UserResponse`), `successMessage`, `errorMessage`.

Each user has `fullName`, `email`, `role`, `helperStatus`. Phone numbers and request content are not shown.

Account labels:

| Condition | Label |
|---|---|
| role ADMIN | Admin |
| helperStatus VERIFIED | Verified helper |
| helperStatus PENDING | Helper waiting for approval |
| helperStatus REMOVED | Helper, removed by an admin |
| helperStatus NONE | Customer |

## Rules for every page

- Shared layout: `<head th:replace="~{fragments/layout :: head('Title')}">` and `<header th:replace="~{fragments/layout :: nav}">`.
- `lang="en"`, one `<h1>`, `<main id="main-content" class="container">`.
- Every input has a `<label>` with `for` matching the input's `id`. Inside a list, make ids unique: `rating-{id}`, `reason-{id}`.
- Only classes from `static/css/app.css` and Bootstrap. The head fragment is replaced as a whole, so a page cannot add its own stylesheet.
- Contact box: `<div class="alert alert-success contact-box">`, shown only when `counterpartName` is not null.
- Do not show phone numbers to admins. Do not show the customer's name on open requests.
