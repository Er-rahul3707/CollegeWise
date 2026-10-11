# Testing and verification

Automated tests use in-memory H2 in MySQL compatibility mode. H2 is test-scope only and does not verify the production MySQL connection.

`ApiIntegrationTest` exercises registration and login, JWT-protected profile retrieval/update and dashboard access, recommendation scores/reasons/admission compatibility, recommendation history, favorites add/remove, valid comparisons and the three-college limit, student/admin role checks, admin statistics, and create/update/delete for college, course, branch, cutoff, and placement records. Test passwords and the test JWT signing key are generated at runtime; none are live credentials. `JwtServiceTest` exercises runtime-key token issuance/parsing and short-key rejection. `RecommendationServiceTest` exercises deterministic scoring, cutoff compatibility, placement contributions, and missing evidence.

Run the requested checks from the project root:

```powershell
mvn clean test
mvn clean package
```

For live MySQL verification, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`, then start the packaged app and check `GET /api/health` and the college catalogue. To test real interactive student/admin journeys, configure the local account variables described in [SETUP_GUIDE.md](SETUP_GUIDE.md); automated H2 tests do not substitute for those live-account checks.
