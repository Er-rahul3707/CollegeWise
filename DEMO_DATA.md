# CollegeWise demo catalogue

The application seeds 20 engineering institutions on startup by default. Set `SEED_DEMO_DATA=false` to disable this catalogue in an environment where demo records are not wanted. The catalogue seed is idempotent: it finds colleges and their catalogue relationships before inserting. It does not create student accounts. A separate local demo student bootstrap is available only with the Spring `demo` profile and configured `DEMO_STUDENT_EMAIL` / `DEMO_STUDENT_PASSWORD` environment variables; see [SETUP_GUIDE.md](SETUP_GUIDE.md).

The ranking values and displayed institution names/cities follow the NIRF 2025 Engineering ranking. The programme, tuition, placement, hostel and admission cutoff values are presentation estimates, not official published statistics or admission advice. Confirm current information with the institution before relying on it. BITS Pilani is included without a rank cutoff because its BITSAT selection uses scores and this schema stores closing ranks.

Reference for the ranking list: [NIRF India Rankings 2025 — Engineering](https://www.nirfindia.org/Rankings/2025/EngineeringRanking.html). Institute websites are stored with each college for follow-up research.

Seeded catalogue relationships include two course types, four common engineering branches per college, JEE/VITEEE/SRMJEEE/WBJEE/TNEA illustrative closing-rank examples where the schema supports them, and one illustrative 2025 placement summary per college.
