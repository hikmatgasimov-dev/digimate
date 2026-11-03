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