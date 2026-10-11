# Data flow

```mermaid
flowchart LR
 S[Student or visitor] --> UI[Browser UI]
 A[Administrator] --> UI
 UI -->|REST and JSON, optional JWT| C[Spring MVC controllers]
 C --> V[Validation and services]
 V --> R[Spring Data JPA]
 R --> DB[(MySQL)]
 DB --> R --> V --> C --> UI
```
