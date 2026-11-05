# Page contract: help request pages (customer side)

The names below are fixed. Controllers put exactly these model attributes; templates read exactly these names.
Templates: `src/main/resources/templates/request-new.html` and `requests-my.html`.
Both use the shared layout (`fragments/layout :: head(...)` and `:: nav`) and still show sample data;
the controller card replaces the sample values with the `th:` bindings below.
The plain `<link>` and `href` inside `th:` elements are only for opening the file directly; Thymeleaf replaces them.

## Routes

| Method | URL | Service call | Result |
|---|---|---|---|
| GET  | `/requests/new` | none | `request-new` with an empty form |
| POST | `/requests` | `create(dto, username)` | valid: redirect to `/requests/my` with `successMessage`; invalid: `request-new` again with errors |
| GET  | `/requests/my` | `findMine(username)` | `requests-my` |
| GET  | `/requests/{id}` | `findById(id, username)` | detail page (later card) |
| GET  | `/requests/{id}/edit` | `findById(id, username)` | `request-new` filled in, form posts to `/requests/{id}` |
| POST | `/requests/{id}` | `update(id, dto, username)` | redirect to `/requests/my` with `successMessage` |
| POST | `/requests/{id}/cancel` | `cancel(id, username)` | redirect to `/requests/my` with `successMessage` |

`username` is the logged-in email (`Principal.getName()` or `@AuthenticationPrincipal`).
All forms use `th:action`, so Spring adds the CSRF token.

## request-new.html

Model attributes:

| Name | Type | Notes |
|---|---|---|
| `helpRequest` | `HelpRequestDto` | form object, `th:object="${helpRequest}"`; on errors, the `BindingResult` follows it |
| `categories` | `List<CategoryResponse>` | `id` and `name`, for the select |
| `editId` | `Long` or absent | set only on the edit page; form action is then `/requests/{editId}` |

Form fields (`name` = `id` = DTO field):

| Field | Input | Rules (from `HelpRequestDto`) |
|---|---|---|
| `title` | `input type="text"` | required, max 100 |
| `description` | `textarea` | required, max 1000, `@NoSensitiveData` |
| `categoryId` | `select`, options from `categories` | required |
| `preferredTime` | `input type="datetime-local"`, value format `yyyy-MM-dd'T'HH:mm` | required, in the future |
| `mode` | two radios, values `IN_PERSON` and `ONLINE` | required |
| `place` | `input type="text"` | optional, max 255 |

Errors: one `<p class="field-error" th:errors="*{field}">` under each field, and `aria-invalid="true"` on the invalid input.

`preferredTime` needs `@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")` in `HelpRequestDto`, otherwise Spring cannot read the `datetime-local` value.

## requests-my.html

Model attributes:

| Name | Type | Notes |
|---|---|---|
| `requests` | `List<HelpRequestResponse>` | newest first; empty list shows "You have no requests yet." |
| `successMessage` | `String` or absent | flash attribute after create, update or cancel |

Fields used per item (`HelpRequestResponse`): `id`, `title`, `categoryName`, `mode`, `place`, `preferredTime`,
`createdAt`, `status`, `counterpartName`, `counterpartPhone`, `counterpartEmail`, `reviewRating`, `reviewComment`.

Display rules:

| Status | Shown | Buttons |
|---|---|---|
| `OPEN` | "Waiting for a helper" | View, Edit, Cancel request |
| `ACCEPTED` | helper name, phone, email | View, Cancel request |
| `COMPLETED` | rating and comment if reviewed | View, plus "Rate the help" when `reviewRating` is null |
| `CANCELLED` | nothing extra | View |

- Buttons always include the base class: `class="btn"`, `class="btn btn-secondary"`, `class="btn btn-danger"`.
- Status badge: `class="status status-open"` (also `status-accepted`, `status-completed`, `status-cancelled`).
- Show the counterpart block only when `counterpartName` is not null. The service decides, not the page.
- `createdAt` is an `Instant` (UTC). Convert it to the app's time zone when formatting.

## Rules for every page

- Shared layout: `<head th:replace="~{fragments/layout :: head('Title')}">` and `<header th:replace="~{fragments/layout :: nav}">`.
- `lang="en"`, one `<h1>`, `<main id="main-content" class="container">`.
- Every input has a `<label>` with `for` matching the input's `id`. Inside a list, make ids unique: `rating-{id}`.
- Only classes from `static/css/app.css`. The head fragment is replaced as a whole, so a page cannot add its own stylesheet.
- Contact box: `<div class="success">`, shown only when `counterpartName` is not null.
- Messages after a redirect (flash attributes): `successMessage` in `<output class="success">`, `errorMessage` in `<p class="error" role="alert">`.
## Review form (in requests-my.html)

Shown for `COMPLETED` requests when `reviewRating` is null. Otherwise the page shows the rating and comment.

| Method | URL | Service call | Result |
|---|---|---|---|
| POST | `/requests/{id}/review` | `ReviewService.create(id, dto, username)` (to be built) | redirect `/requests/my`, `successMessage` |

| Form field | Input | Rules |
|---|---|---|
| `rating` | `select`, values 1 to 5, `id="rating-{id}"` | required, 1 to 5 |
| `comment` | `textarea`, `id="comment-{id}"` | optional, max 1000 |

Suggested DTO: `ReviewDto(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) @NoSensitiveData String comment)`.

Extra model attribute for `requests-my`: `errorMessage` (`String` or absent, flash), e.g. when the request was already reviewed.

Final buttons on `requests-my` (they replace the "Buttons" column above):

| Status | Actions |
|---|---|
| `OPEN` | Edit (link to `/requests/{id}/edit`), Cancel request (POST, `btn btn-danger`) |
| `ACCEPTED` | none (cancel is only allowed while `OPEN`, as in `HelpRequestService.cancel`) |
| `COMPLETED` | review form, or the rating if reviewed |
| `CANCELLED` | none |

## Edit page

`GET /requests/{id}/edit` renders `request-new.html` with `helpRequest` filled in and `editId` set.
The `<h1>` says "Edit request" and the form posts to `/requests/{editId}`.
`place` is required when `mode` is `IN_PERSON` (checked in the service, also on update).

## helper-inbox.html (`HelperPageController`)

| Method | URL | Service call | Result |
|---|---|---|---|
| GET  | `/helper` | `HelperFlowService.findOpen(username)` and `findMine(username)` (to be built) | `helper-inbox` |
| POST | `/helper/requests/{id}/accept` | `HelperFlowService.accept(id, username)` | redirect `/helper`, `successMessage` |
| POST | `/helper/requests/{id}/complete` | `HelperFlowService.complete(id, username)` | redirect `/helper`, `successMessage` |

| Model attribute | Type | Notes |
|---|---|---|
| `openRequests` | `List<HelpRequestResponse>` | status `OPEN` only; contact fields are always null |
| `myRequests` | `List<HelpRequestResponse>` | requests this helper accepted (`ACCEPTED` and `COMPLETED`) |
| `successMessage` | `String` or absent | flash |
| `errorMessage` | `String` or absent | flash, e.g. "This request was already accepted." |

| Section | Shown per request | Action |
|---|---|---|
| Open requests | title, description, category, mode, place, preferred time | Accept (POST, `btn`) |
| My accepted requests | status badge, contact box with the customer | Mark as completed (POST, `btn`), only while `ACCEPTED` |

Only verified helpers may open `/helper`. The accept method checks the status again (`OPEN` only);
if another helper was faster, redirect back with `errorMessage`.

## Browser authentication pages

Templates:
- `src/main/resources/templates/login.html`
- `src/main/resources/templates/register.html`

### Routes

| Method | URL | Result |
|---|---|---|
| GET | `/login` | Render `login` |
| POST | `/login` | Spring Security processes login |
| GET | `/register` | Render `register` |
| POST | `/register` | Register using `UserService` |
| POST | `/logout` | Spring Security logs out |

### login.html

- Form action: `@{/login}`
- Form method: POST
- Email input: `name="username"`
- Password input: `name="password"`
- `param.error`: invalid credentials, class `error`
- `param.logout`: successful logout, class `success`
- `param.registered`: successful registration, class `success`

### register.html

Model attribute:
- `form`: `RegisterRequest`

Form action: `@{/register}`
Form method: POST

Fields:
- `fullName`
- `email`
- `phone`
- `password`
- `wantsToHelp`: boolean checkbox, unchecked by default

Validation:
- Show field errors using `th:errors`
- Error class: `field-error`
- Duplicate email errors belong to field `email`
- Invalid submissions redisplay `register.html`

Successful registration redirects to `/login?registered`.

Both templates use the shared `head` and `nav` fragments,
`lang="en"`, one `h1`, and
`<main id="main-content" class="container">`.
