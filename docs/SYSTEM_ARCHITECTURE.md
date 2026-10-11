# System architecture

The browser calls same-origin Spring MVC REST APIs. Controllers, validated DTOs, services, repositories, and JPA entities are separated by responsibility. Spring Security verifies signed JWTs per request. MySQL stores application state. See [PROJECT_ARCHITECTURE.md](../PROJECT_ARCHITECTURE.md).
