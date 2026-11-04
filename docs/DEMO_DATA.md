# Demo data

Every computer starts with the same demo accounts and categories.
`DataInitializer` (`com.digimate.config`) adds them at startup.

## Setup

Add `DEMO_PASSWORD` next to the database variables (value: ask in the team chat, at least 8 characters).

IntelliJ: Run > Edit Configurations > DigimateApplication > Environment variables:

```
DEMO_PASSWORD=...
```
Must be 8 charecters long.

Start the application. The log shows:

```
Demo data ready: 7 users and 5 categories created
```
## Accounts

All accounts use `DEMO_PASSWORD`. Log in with the email.

| Email                     | Role  | Helper status | Use it for                                              |
|---------------------------|-------|---------------|---------------------------------------------------------|
| `admin@digimate.test`     | ADMIN | NONE          | helper verification                                     |
| `helper@digimate.test`    | USER  | VERIFIED      | accepting requests                                      |
| `pending@digimate.test`   | USER  | PENDING       | the admin's pending list                                |
| `customer1@digimate.test` | USER  | NONE          | creating requests                                       |
| `customer2@digimate.test` | USER  | NONE          | a second customer                                       |
| `helper2@digimate.test`   | USER  | VERIFIED      | a second helper (two helpers competing for one request) |
| `customer3@digimate.test` | USER  | NONE          | a third customer                                        |

## Categories

Smartphone, Computer, Online banking, Email, Video calls.

## Shortcuts to check in pgAdmin

```
SELECT email, role, helper_status FROM users ORDER BY id;
SELECT name FROM category ORDER BY id;
```

## Change or reset

Existing rows are never overwritten. After changing an account in the code, update or delete
the row in your local database, then restart:

```
DELETE FROM users WHERE email = 'customer2@digimate.test';
```
```
UPDATE users
SET helper_status = 'NONE'
WHERE email = 'customer3@digimate.test';
```

If the account already has requests, delete those first, or recreate the whole local database.