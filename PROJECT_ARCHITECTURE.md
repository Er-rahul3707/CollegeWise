# CollegeWise — Smart College Comparison & Career Guidance System

**Current Java Spring Boot implementation by Rahul Chandra Sharma**

## Project architecture

```text
Browser (HTML/CSS/Bootstrap/vanilla JS)
        │ same-origin JSON REST / bearer token
Spring MVC controllers → DTO validation → services → Spring Data JPA → MySQL 8+
```

Controllers translate requests. Services own business rules. Repositories use database-side filtering and pagination. Request records constrain API input. `SecurityConfig` is stateless and uses a JWT filter. Passwords use BCrypt. Central advice maps validation and request errors to JSON.

The application is one Spring Boot deployable. Static assets are packaged in `resources/static`, so there is no separate frontend server or Node.js runtime. Controllers return response DTOs rather than JPA entities to prevent relationship traversal and sensitive data exposure.
