# Setup guide

Prerequisites: Java 17+, Maven 3.9+, MySQL 8+.

Create a local database and application user in MySQL, replacing the password placeholder with a unique local value:

```sql
CREATE DATABASE collegewise CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'collegewise'@'localhost' IDENTIFIED BY 'replace-with-a-local-password';
GRANT ALL PRIVILEGES ON collegewise.* TO 'collegewise'@'localhost';
```

Set the non-secret connection values in PowerShell. Keep the database password and JWT signing key out of command history; the masked setup below stores them as Windows User environment variables. The app uses MySQL at runtime; tests use an isolated H2 database.

```powershell
[Environment]::SetEnvironmentVariable('DB_URL', 'jdbc:mysql://localhost:3306/collegewise?serverTimezone=UTC', 'User')
[Environment]::SetEnvironmentVariable('DB_USERNAME', 'collegewise', 'User')

foreach ($name in @('DB_PASSWORD', 'JWT_SECRET')) {
    $secure = Read-Host "Enter $name" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $value = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        [Environment]::SetEnvironmentVariable($name, $value, 'User')
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        Remove-Variable value -ErrorAction SilentlyContinue
    }
}
```

Set `ADMIN_EMAIL` and `DEMO_STUDENT_EMAIL` separately as normal text values if using those bootstrap accounts. Do not set or store passwords you do not intend to use. `JWT_SECRET` must be a random value with at least 32 bytes.

### Local demo accounts

There are no default credentials. The existing registration endpoint remains the normal way to create student accounts. For a repeatable local presentation account, start with the `demo` Spring profile and set `DEMO_STUDENT_EMAIL` and `DEMO_STUDENT_PASSWORD`. That profile provisions the student only when the email is new, creates an empty profile, and never resets an existing account's password. The account mechanism is profile-gated; do not activate the `demo` profile in a deployed environment.

Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` for first-time administrator provisioning. At startup, the app creates an admin when that email is new; it does not change an existing account or its password. Both demo student and admin passwords must be 12–72 characters. Credentials remain in Windows User environment variables and are never embedded in source or returned by the API. Do not add credential values to `.env.example` beyond placeholders or commit local secret files.

In PowerShell, run the following interactively. Email values are entered as normal text; each password prompt is masked. The password is stored in the Windows User environment without being echoed. Repeat with a new PowerShell window if you change a stored value.

```powershell
[Environment]::SetEnvironmentVariable('ADMIN_EMAIL', 'admin@example.com', 'User')
[Environment]::SetEnvironmentVariable('DEMO_STUDENT_EMAIL', 'student-demo@example.com', 'User')
```

Only if provisioning an admin or demo student, set the corresponding password with a masked prompt:

```powershell
$name = 'ADMIN_PASSWORD' # Use DEMO_STUDENT_PASSWORD only for the demo student account
$secure = Read-Host "Enter $name" -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $value = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    [Environment]::SetEnvironmentVariable($name, $value, 'User')
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    Remove-Variable value -ErrorAction SilentlyContinue
}
```

Open a **new** PowerShell window so it inherits the saved values. Keep the existing `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` configured there. Stop any old application process first; then start the MySQL-backed app with the demo profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
mvn spring-boot:run
```

The default app URL is `http://localhost:8080/`; the health check is `http://localhost:8080/api/health`. Student registration and sign-in are available from the home page's Sign in modal. Admin sign-in is at `http://localhost:8080/admin-login.html`; the API endpoint is `POST /api/auth/admin/login`. Do not infer successful authorization from the UI alone: admin APIs require the server-validated administrator role.

The admin bootstrap is enabled in every profile when both admin variables are present. The demo student bootstrap runs only in the `demo` profile. For a packaged run, use `java -jar target\collegewise-1.0.0.jar --spring.profiles.active=demo` in that same new window. Registration remains available at the landing page for other student accounts. Keep application database credentials in environment variables; do not commit local config files. Hibernate `ddl-auto=update` is for this capstone; production should use reviewed migrations.

After the application starts, use **Admin** in the top navigation or open `http://localhost:8080/admin-login.html` (use your configured port if `SERVER_PORT` is set). Enter the same `ADMIN_EMAIL` and `ADMIN_PASSWORD` that were available when the application first created the account. The dedicated login endpoint rejects student accounts with HTTP 403, rejects invalid credentials with HTTP 401, and sets the same HttpOnly authentication cookie for a valid admin. `/admin.html` and `/api/admin/**` remain protected by the server. Bootstrap does not replace the password of an existing account; if the configured email already belongs to an admin and the password is unknown, do not expect changing the environment variable to reset it. Use an isolated local test schema for a new disposable admin or follow your deliberate account-recovery procedure.

Run the tests and build from the repository root. Tests use H2 and do not connect to MySQL or seed production data.

```powershell
mvn clean test
mvn clean package
```

Start the live MySQL application from a new PowerShell window that inherited the User environment variables:

```powershell
mvn spring-boot:run
```

Open http://localhost:8080. Health endpoint: `GET /api/health`. The checked-in demo catalogue seeds when `SEED_DEMO_DATA` is enabled (default). College fees, placements, and admission cutoffs are marked as illustrative estimates in the UI; validate them with the institution before relying on them. See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for the route list and bearer-token examples.
## Safe local end-to-end verification

Use a separate MySQL schema for registration and admin CRUD tests. Do not run those workflows against production data. In MySQL Workbench or another administrative SQL client, create a test schema and grant the local application user access:

```sql
CREATE DATABASE collegewise_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON collegewise_test.* TO 'collegewise'@'localhost';
```

In a fresh PowerShell window, point the application at that schema. Keep the already configured `DB_PASSWORD` and `JWT_SECRET` in the process environment; do not place their values in commands or files.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/collegewise_test'
$env:DB_USERNAME = 'collegewise'
$env:SERVER_PORT = '8082'
$env:DDL_AUTO = 'update'
$env:SEED_DEMO_DATA = 'true'
$env:SPRING_PROFILES_ACTIVE = 'default'
$env:ADMIN_EMAIL = 'admin-local@example.test'

$secure = Read-Host 'Temporary local admin password' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}

mvn spring-boot:run
```

The admin is created only if `ADMIN_EMAIL` is new; startup never resets an existing password. For a different admin password, use a fresh test schema or configure the existing local account deliberately. No default password is supplied.

Open `http://localhost:8082/`. Register a unique test-only email, then verify login, profile update, recommendations/history, comparison (two or three colleges), favorites, and logout. Sign out, then use the configured admin email and the password entered at the masked prompt to verify admin CRUD. Student and admin records created this way belong only to `collegewise_test`. After finishing, stop the app and remove the test schema only when you no longer need its data.
