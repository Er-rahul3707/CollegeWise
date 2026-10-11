# Testing and verification

Automated tests use in-memory H2 in MySQL compatibility mode. H2 is test-scope only and does not verify the production MySQL connection.

`ApiIntegrationTest` exercises registration and login, JWT-protected profile retrieval/update and dashboard access, recommendation scores/reasons/admission compatibility, recommendation history, favorites add/remove, valid comparisons and the three-college limit, student/admin role checks, admin statistics, and create/update/delete for college, course, branch, cutoff, and placement records. Test passwords and the test JWT signing key are generated at runtime; none are live credentials. `JwtServiceTest` exercises runtime-key token issuance/parsing and short-key rejection. `RecommendationServiceTest` exercises deterministic scoring, cutoff compatibility, placement contributions, and missing evidence.

Run the requested checks from the project root:

```powershell
mvn clean test
mvn clean package
```

For live MySQL verification, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`, then start the packaged app and check `GET /api/health` and the college catalogue. To test real interactive student/admin journeys, configure the local account variables described in [SETUP_GUIDE.md](SETUP_GUIDE.md); automated H2 tests do not substitute for those live-account checks.
## Verification results — 2026-10-11

- **Automated tests: PASS.** `mvn clean test` ran 14 tests with 0 failures and 0 errors. These tests use isolated in-memory H2.
- **Package build: PASS.** `mvn clean package` completed successfully.
- **JavaScript syntax: PASS.** `node --check` passed for `app.js`, `dashboard.js`, `college.js`, and `admin.js`.
- **Live MySQL API workflow: PASS.** The packaged app ran against a uniquely named temporary schema on the local MySQL server. Schema creation, JPA initialization, and the 20-college seed catalogue were verified. The temporary schema was dropped afterward; the configured `collegewise` schema was not used for application writes.
- **Student flow: PASS.** Registration, invalid input, login, authenticated profile retrieval/update, HttpOnly/SameSite cookie attributes, logout expiry, recommendations with reasons and cutoff compatibility, history, two/three-college comparisons, rejection of four colleges, and favorites add/list/remove were exercised against MySQL.
- **Admin flow: PASS.** A process-only `ADMIN_EMAIL`/random `ADMIN_PASSWORD` bootstrap was used. Admin login, dashboard statistics, college CRUD, course/branch/cutoff/placement create/update/list operations, invalid cutoff/placement rejection, and student/admin role separation were exercised. A student was denied admin and college-write operations.
- **Browser UI authenticated journey: NOT VERIFIED in this run.** The live checks used same-origin HTTP API clients with cookie jars. They did not automate all dashboard/admin interactions in a browser.
- **Admission data: LIMITATION.** Recommendations are rule-based fit scores with reasons and cutoff compatibility. Demo fees, placement values, and cutoffs are illustrative and do not guarantee admission.

No application source or account data in the configured `collegewise` schema was changed during this run. Test credentials were generated in process memory and were not printed or persisted.

## Release audit — 2026-10-12

- **Automated tests: PASS.** `mvn clean test` ran 14 tests with zero failures, errors, or skips. Spring test logs confirmed the tests used in-memory H2.
- **Package build: PASS.** `mvn clean package` completed successfully.
- **JavaScript syntax: PASS.** `node --check` passed for `app.js`, `dashboard.js`, `college.js`, and `admin.js`.
- **MySQL startup and schema: PASS.** The packaged app connected to a newly created isolated `collegewise_test` schema on localhost, initialized JPA, seeded the 20-college catalogue, and returned HTTP 200 from `/api/health`. The configured `collegewise` schema was not targeted.
- **Student browser workflow: PASS.** In the actual browser, a disposable student registered, logged in, updated academic and preference data, generated recommendations with reasons and `MEETS CUTOFF` compatibility, viewed history and college details, saved and removed a favorite, compared three colleges, received validation for four, logged out, and logged in again.
- **College discovery browser workflow: PASS.** Search, state/city, fee, hostel, and ranking filters returned the expected college. College details showed fees, ranking, hostel, courses, branches, illustrative cutoff and placement rows.
- **Admin browser workflow: PARTIAL.** Admin sign-in, dashboard statistics, college create/edit, and course/branch/cutoff/placement create/edit passed. The stats refresh defect found during this audit is fixed in the packaged app; counters refreshed after saving and on Reload. The UI Delete button was not activated. The isolated schema was removed after testing. Automated H2 regression coverage verifies protected college deletion and cascade cleanup; the UI delete click itself is NOT VERIFIED. Admin-variable bootstrap and live API authorization/CRUD were exercised in the prior isolated MySQL API audit recorded above.
- **Browser diagnostics: PASS for this session.** No console errors from `localhost:8082` were observed. The visible browser viewport was 714 CSS pixels wide with no horizontal overflow; a dedicated narrow-phone viewport was not tested. Browser extension warnings were outside the application origin.
- **Data scope: PASS.** Only `collegewise_test` was used for these live changes. The production/application `collegewise` schema was not used. The test schema and its disposable account/catalogue were removed after the audit; the configured `collegewise` schema remains untouched. Never point demo CRUD checks at production.

The browser used the local Spring Boot app at `http://localhost:8082`. Screenshots and mobile-specific rendering are not part of this automated report.
