# API documentation

JSON APIs are rooted at `/api`. Use `Authorization: Bearer <token>` for authenticated calls. Admin routes require `ROLE_ADMIN`; registration only creates students.

## Authentication

- `POST /auth/register` `{ "name": "A Student", "email": "student@example.com", "password": "at-least-12-chars" }`
- `POST /auth/login` `{ "email": "student@example.com", "password": "..." }` → bearer token, role, display name
- `POST /auth/logout` → client discards token; no server-side revocation

## Discovery

- `GET /colleges?page=0&size=12&q=tech&state=Karnataka&city=Bengaluru&maxFees=250000&hostel=true&sort=fees`
- `GET /colleges/search?q=engineering&page=0&size=12`
- `GET /colleges/{id}`
- Admin: `POST|PUT|DELETE /colleges...` and `/admin/colleges...`

## Student

- `GET|PUT /students/profile`
- `GET /students/recommendations`
- `GET /students/recommendations/history` (latest 50)
- `GET /students/dashboard`, `GET /students/career-guidance`
- `GET /students/favorites`, `POST|DELETE /students/favorites/{collegeId}`
- `GET /students/compare?ids=1&ids=2` (2–3 distinct IDs)

## Admin

- `GET /admin/dashboard`
- `GET|POST /admin/courses`, `PUT|DELETE /admin/courses/{id}`
- `GET|POST /admin/branches`, `PUT|DELETE /admin/branches/{id}`
- `GET|POST /admin/cutoffs`, `PUT|DELETE /admin/cutoffs/{id}`
- `GET|POST /admin/placements`, `PUT|DELETE /admin/placements/{id}`
- `GET /admin/users?page=0&size=25`, `PATCH /admin/users/{id}/active?active=false` (soft-disable; cannot disable the current admin)

Validation errors include field messages; unexpected errors do not return stack traces. See `POSTMAN_COLLECTION.json` for request examples.
