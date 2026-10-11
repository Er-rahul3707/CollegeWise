# Setup guide

Prerequisites: Java 17+, Maven 3.9+, MySQL 8+.

Create a local database and application user in MySQL, replacing the password placeholder with a unique local value:

```sql
CREATE DATABASE collegewise CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'collegewise'@'localhost' IDENTIFIED BY 'replace-with-a-local-password';
GRANT ALL PRIVILEGES ON collegewise.* TO 'collegewise'@'localhost';
```

Configure PowerShell for the current session. The app uses the MySQL driver and MySQL dialect; only tests use H2.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/collegewise?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME = 'collegewise'
$env:DB_PASSWORD = 'your-local-password'
$env:JWT_SECRET = 'generate-a-random-secret-of-at-least-32-bytes'
```

### Local demo accounts

There are no default credentials. The existing registration endpoint remains the normal way to create student accounts. For a repeatable local presentation account, start with the `demo` Spring profile and set `DEMO_STUDENT_EMAIL` and `DEMO_STUDENT_PASSWORD`. That profile provisions the student only when the email is new, creates an empty profile, and never resets an existing account's password. The account mechanism is profile-gated; do not activate the `demo` profile in a deployed environment.

Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` for first-time administrator provisioning. At startup, the app creates an admin when that email is new; it does not change an existing account or its password. Both demo student and admin passwords must be 12–72 characters. Values stay in Windows User environment variables and are never embedded in source or returned by the API. Do not add credential values to `.env.example` beyond placeholders or commit local secret files.

In PowerShell, run the following interactively. Email values are entered as normal text; each password prompt is masked. The password is stored in the Windows User environment without being echoed. Repeat with a new PowerShell window if you change a stored value.

```powershell
$envNames = @('ADMIN_EMAIL', 'DEMO_STUDENT_EMAIL')
foreach ($name in $envNames) {
    $value = Read-Host "Enter $name"
    [Environment]::SetEnvironmentVariable($name, $value, 'User')
}
foreach ($name in @('ADMIN_PASSWORD', 'DEMO_STUDENT_PASSWORD')) {
    $secure = Read-Host "Enter $name" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        [Environment]::SetEnvironmentVariable($name, $plain, 'User')
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        Remove-Variable plain -ErrorAction SilentlyContinue
    }
}
```

Open a **new** PowerShell window so it inherits the saved values. Keep the existing `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` configured there. Stop any old application process first; then start the MySQL-backed app with the demo profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
mvn spring-boot:run
```

The admin bootstrap is enabled in every profile when both admin variables are present. The demo student bootstrap runs only in the `demo` profile. For a packaged run, use `java -jar target\collegewise-1.0.0.jar --spring.profiles.active=demo` in that same new window. Registration remains available at the landing page for other student accounts. Keep application database credentials in environment variables; do not commit local config files. Hibernate `ddl-auto=update` is for this capstone; production should use reviewed migrations.

```powershell
mvn clean test
mvn clean package
mvn spring-boot:run
```

Open http://localhost:8080. Health endpoint: `GET /api/health`. The checked-in demo catalogue seeds when `SEED_DEMO_DATA` is enabled (default). College fees, placements, and admission cutoffs are marked as illustrative estimates in the UI; validate them with the institution before relying on them.
