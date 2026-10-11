# CollegeWise — Smart College Comparison & Career Guidance System

**Developed by Rahul Chandra Sharma & Team**

CollegeWise helps students explore Indian engineering colleges, compare options, and review profile-based recommendations with visible reasons and admission-cutoff compatibility. It is a Java full stack capstone: one Spring Boot application serves the REST API and its packaged web interface.

## Features

- Student registration, sign-in, BCrypt password storage, JWT authentication, and profile management.
- Paginated college discovery with text search, state/city, fee, hostel, and sorting filters.
- College detail pages with related courses, branches, cutoff examples, and placement summaries.
- Profile-aware recommendations with match scores, reasons, concerns, cutoff compatibility, and history.
- Student favorites and college comparison for two or three colleges.
- Role-protected administrator dashboard and CRUD for colleges, courses, branches, cutoffs, and placements.
- Twenty restart-safe demo colleges with related catalogue data, enabled by default. Set `SEED_DEMO_DATA=false` to disable demo catalogue seeding.

Demo fees, placement figures, and cutoffs are illustrative estimates, not official admissions or outcomes. Confirm current information with each institution. The catalogue notes its NIRF 2025 ranking reference and links to institution websites.

## Technology and architecture

- **Frontend:** HTML5, CSS3, Bootstrap 5, and vanilla JavaScript. Static assets are served by Spring Boot; no Node.js runtime or separate frontend server is needed.
- **Backend:** Java 17, Spring Boot 3.3.5, Spring MVC, Spring Security, JWT, Jakarta Validation, Spring Data JPA, and Hibernate.
- **Application database:** MySQL 8+ using MySQL Connector/J. H2 is test-scope only and is not a runtime fallback.
- **Build:** Maven 3.9+.

```text
Browser → same-origin REST/JSON → Spring MVC + Security/JWT
        → services → Spring Data JPA/Hibernate → MySQL
```

See [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md), [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md), and [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for implementation details.

## Database and local setup

Install JDK 17+, Maven 3.9+, and MySQL 8+. Create the `collegewise` database and a local MySQL user, then configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a random `JWT_SECRET` of at least 32 bytes. Spring Boot does not automatically load `.env`; use process or Windows User environment variables. Do not commit real credentials.

The app defaults to `jdbc:mysql://localhost:3306/collegewise`; see [SETUP_GUIDE.md](SETUP_GUIDE.md) for the complete MySQL setup, PowerShell instructions, demo student bootstrap, and admin bootstrap. There are no committed/default account credentials. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` before the first start to create an administrator. To provision a reusable local student demo account, use the `demo` Spring profile and set `DEMO_STUDENT_EMAIL` and `DEMO_STUDENT_PASSWORD`; this profile is for local development/presentations only.

Run:

```powershell
mvn clean test
mvn clean package
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). The JSON API is rooted at `/api`; the catalogue health check is `GET /api/health`. H2 is used only by automated tests. For a packaged build, run `java -jar target\collegewise-1.0.0.jar` after setting the required environment variables.

## Verification and limits

Automated coverage is described in [TESTING.md](TESTING.md). H2-backed tests verify application behavior, not a live MySQL instance; run the application against configured MySQL for a live connection check. The demo recommendation score is a rule-based guide and must not be interpreted as admission assurance. Email verification, password reset, token revocation, and refresh tokens are not implemented. Stateless logout clears the browser token.

## Attribution

**CollegeWise — Smart College Comparison & Career Guidance System**
**Developed by Rahul Chandra Sharma & Team**

Preserve any third-party notices supplied with dependencies or source material. This project attribution does not imply authorship of third-party work.
