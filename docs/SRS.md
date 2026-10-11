# Software requirements summary

CollegeWise supports college discovery through paginated search, profile-aware recommendation, comparison, favorites, and recommendation history. Students register, authenticate, and maintain academic and preference data. Administrators manage college and catalog data and can enable/disable accounts through protected APIs. The application is a Spring Boot REST server with a packaged Bootstrap/vanilla JavaScript frontend and MySQL persistence.

Non-functional goals include password hashing, role authorization, validation, bounded list sizes, and explainable deterministic scoring. Admission data quality and availability depend on deployment and verified data sources.
