# Database schema

- `users`: unique email, BCrypt hash, role, active flag, timestamps.
- `student_profile`: one-to-one user; academics, entrance performance (`entrance_rank` column), preferences.
- `college`: location, tuition, rank, facilities; indexed by name and location.
- `course`: course catalog.
- `branch`: many-to-one college; unique `(college_id, name)`.
- `cutoff`: college and optional course/branch; exam/year/category and closing rank/percentile.
- `placement`: college and optional course; annual totals and package metrics.
- `favorite`: user-to-college link, unique per pair.
- `recommendation_history`: user-to-college score and timestamp.

MySQL 8+ is the configured application database, using Connector/J and `org.hibernate.dialect.MySQLDialect`. Connection uses `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; there is no H2 runtime fallback. Foreign keys come from `ManyToOne` and `OneToOne` mappings; uniqueness and indexes are declared for email, college name/location, college-branch, cutoff exam/year, placements, favorite pairs, and user recommendation history. Admin deletes remove dependent records transactionally. Hibernate `ddl-auto=update` is for the capstone; production should use reviewed migrations.
