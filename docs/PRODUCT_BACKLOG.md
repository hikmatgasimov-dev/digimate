# DigiMate product backlog

Product Owner: Hikmat. Team: Hikmat , Mykolas, Anil, Maksym.
The Jira board (project SCRUM) is the source of truth. This file is a summary.

## Must have (MVP)
- Registration and login, with an optional "I also want to help others"
- Admin verifies helpers
- Customer creates, views, edits and cancels help requests (privacy warning, sensitive-number check)
- Verified helper browses open requests, accepts and completes them
- Contact details shown to both people only after acceptance
- Customer rates a completed request (1 to 5)
- Swagger documentation, unit tests, seed data, README

## Also delivered
- Customer reports a problem with a helper (one report per request)
- Admin dismisses a report, or removes a helper with a reason; the helper's accepted requests become open again
- Admin users page (no phone numbers)
- Deployment with Docker and GitHub Actions at https://team1.acnbootcamp.lv

## Later (not in this version)
- Notifications, in-app chat, admin statistics, applying to be a helper after registration, mobile layout

## Sprints
- Sprint 0 (2 to 3 Nov): skeleton, entities, security, test setup
- Sprint 1 (4 to 6 Nov): backend core with tests, first end-to-end run
- Sprint 2 (7 to 9 Nov): pages, remaining tests, Swagger, README
- 10 Nov: test day. 11 Nov: buffer, stop merging at 15:00. Code freeze 11 Nov 17:00.