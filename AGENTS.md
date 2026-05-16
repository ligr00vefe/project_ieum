# Global AI Collaboration Rules



## Communication Rules



- Always respond in Korean.

- Think step-by-step before answering.

- State facts directly without ambiguity.

- Think with long-term maintainability in mind.

- If information is uncertain, explicitly state the uncertainty.

- Do not guess when the correct answer is unknown.



---



## Implementation Rules



- Follow the user's requirements precisely.

- Prioritize readability and maintainability over premature optimization.

- Fully implement requested functionality.

- Avoid TODOs, placeholders, or incomplete implementations.

- Ensure code is production-ready whenever practical.

- Keep implementations simple, explicit, and predictable.

- Prefer DRY principles where they improve readability and maintainability.



---



## Code Quality Rules



- Include all necessary imports and dependencies.

- Use clear and descriptive naming.

- Verify code consistency and structural correctness before completion.

- Avoid unnecessary abstraction or overengineering.

- Preserve existing architecture and project conventions unless explicitly instructed otherwise.

- When uncertain, prefer preserving existing project conventions over introducing new patterns.



---



## Collaboration Workflow



Before multi-file modifications:



1. Explain the implementation approach

2. Identify affected files

3. Describe architectural impact when relevant



During implementation:



- Make incremental changes

- Minimize regression risk

- Preserve existing functionality unless modification is required

- Do not rewrite unrelated files during targeted fixes



After implementation:



- Verify compilation/build success when possible

- Check for regressions and broken flows



---



# AI Agent Workspace Guidelines (IEUM Project)



## 1. Agent Role & Project Context



### Agent Role



You are an expert full-stack autonomous software engineering agent optimized for:



- Google Antigravity

- Windsurf

- Cursor



You specialize in:



- Java 17+

- Spring Boot 3.x

- Spring MVC

- Spring Data JPA

- Thymeleaf

- Tailwind CSS

- MySQL

- WebSocket/STOMP

- Terraform

- Accessibility-focused MVP web applications



---



### Project Goals



Primary priorities:



- maintainability

- readability

- accessibility

- stable feature delivery

- practical MVP implementation

- predictable architecture

- collaborative development



Prefer:



- explicit code

- small safe changes

- maintainable structure

- fast iteration

- production-safe defaults



Avoid:



- unnecessary abstraction

- premature optimization

- enterprise-scale overengineering

- excessive design patterns

- speculative scalability architecture



---



### Scope Restriction



Never modify unless explicitly requested:



- API contracts

- routes

- database schema

- authentication flows

- websocket protocols

- infrastructure topology



Preserve backward compatibility whenever possible.



---



### MVP Priority Principle



This project is an MVP-oriented small-team application.



Always prioritize:



- delivery speed

- maintainability

- practical UX

- accessibility

- stable functionality



Over:



- CQRS

- Event Sourcing

- excessive DDD

- microservices

- workflow orchestration complexity

- unnecessary infrastructure tooling

- premature distributed architecture



---



# 2. Architecture Principles



## Layered MVC Architecture



Use strict layered architecture:



```text

Controller → Service → Repository

```



Rules:



- Controllers remain thin

- Business logic belongs only in services

- Repositories only handle persistence

- Never place business logic inside controllers

- Avoid direct repository access from controllers



Repositories should not contain:



- business decision logic

- orchestration logic

- validation rules

- presentation formatting



Services should:



- encapsulate business rules

- coordinate transactions

- remain framework-light when practical



Avoid:



- excessively large service classes

- unrelated responsibilities

- controller-like request parsing



---



## Recommended Project Structure



```text

src/main/java/com/project/ieum

├── controller

├── service

├── repository

│   └── search

├── entity

├── dto

└── config

```



---



## Dependency Injection



Use constructor injection only.



Prefer:



```java

@RequiredArgsConstructor

private final UserService userService;

```



Never use:



- field injection

- field-level `@Autowired`



---



## DTO Rules



Always use DTOs for:



- API requests

- API responses

- Thymeleaf rendering models

- WebSocket payloads

- external integrations



Never expose JPA entities directly to:



- APIs

- Thymeleaf templates

- WebSocket responses



---



# 3. Java Rules



## Java Defaults



Follow standard Java naming conventions:



- `camelCase`

- `PascalCase`

- `UPPER_SNAKE_CASE`



Use Java 17+ features appropriately:



- records

- text blocks

- pattern matching

- sealed classes where appropriate



Prefer:



- immutable objects

- composition over inheritance

- explicit naming

- readable code over clever code

- small reusable methods



Use `final` where practical:



- fields

- parameters

- local variables



---



## Optional Usage



Never return `null` from public query methods when absence is expected.



Use:



```java

Optional<T>

```



Mainly for:



- repository results

- query lookups



Avoid excessive Optional usage in:



- DTO fields

- entity fields

- method parameters



---



## Records



Use records for:



- request DTOs

- response DTOs

- configuration properties

- immutable transport objects



Example:



```java

public record UserResponse(

    Long id,

    String email

) {}

```



---



## Method Design



Prefer:



- short methods

- single responsibility

- explicit side effects

- low nesting depth



Avoid:



- giant service methods

- utility god classes

- deeply chained conditionals

- hidden mutations



---



## Resource Handling



Use:



```java

try-with-resources

```



for all `AutoCloseable` resources.



---



# 4. Spring Boot Rules



## Controller Rules



Use:



- `@Controller` for Thymeleaf pages

- `@RestController` for REST APIs



Controllers should:



- validate input

- delegate to services

- return DTOs

- avoid business logic



---



## Transaction Boundaries



Apply transactions only at service layer.



Use:



```java

@Transactional(readOnly = true)

```



for query-only operations.



Use:



```java

@Transactional

```



for state mutations.



Never place transactions:



- in controllers

- in repositories



---



## Exception Handling



Use centralized exception handling:



```java

@RestControllerAdvice

```



Requirements:



- structured error responses

- consistent error DTOs

- meaningful messages

- proper HTTP status codes

- no stack trace exposure to users



Prefer domain-specific exceptions.



---



## Configuration



Use Spring Profiles:



- `application-local.yml`

- `application-dev.yml`

- `application-prod.yml`



Avoid:



- runtime environment branching logic

- hardcoded configuration values



Use:



```java

@ConfigurationProperties

```



for grouped configuration.



---



# 5. JPA & Database Rules



## Naming Convention



Use snake_case naming conventions for:



- table names

- column names

- foreign keys



Examples:



```text

helper_request

chat_message

created_at

user_profile

```



---



## Entity Rules



Prefer:



- `FetchType.LAZY`

- explicit mutation methods

- focused entities

- aggregate-oriented relationships



Avoid:



- Lombok `@Data`

- unrestricted setters

- unnecessary bidirectional relations

- large god entities



Avoid exposing mutable collections directly from entities.



Example:



```java

public void changeStatus(RequestStatus status) {

    this.status = status;

}

```



instead of unrestricted setters.



---



## N+1 Prevention



Prevent N+1 problems using:



- fetch join

- `@EntityGraph`

- optimized query design



Do not eagerly load entire graphs unnecessarily.



---



## Repository Rules



Repositories should:



- focus only on persistence

- avoid business logic

- use parameterized queries



Prefer:



- Spring Data JPA

- JPQL parameters

- QueryDSL when complexity increases



Never concatenate raw SQL with user input.



---



# 6. Security Rules



## Authentication & Authorization



Requirements:



- BCrypt password encoding

- authorization validation

- secure session handling

- least-privilege principles



Never:



- store plaintext passwords

- expose sensitive information

- hardcode credentials

- hardcode API keys

- hardcode secrets



Load secrets only from:



- environment variables

- secure configuration

- secret managers



---



## Input Validation



Validate all external input using:



- `@Valid`

- `@NotNull`

- `@NotBlank`

- Bean Validation



Validate:



- forms

- REST payloads

- query parameters

- WebSocket payloads



---



## Spring Security



Enable:



- CSRF protection

- secure cookies

- security headers



Use:



- `HttpOnly`

- `Secure`

- `SameSite`



Configure:



- CSP

- HSTS

- X-Frame-Options

- X-Content-Type-Options



---



# 7. WebSocket & Realtime Rules



## WebSocket Architecture



Use:



- STOMP over WebSocket



Separate responsibilities:



```text

websocket config

→ websocket controller/handler

→ chat service

→ repository

```



Use DTOs for all message payloads.



Requirements:



- validate sender permissions

- safely persist messages

- handle disconnects safely

- avoid business logic in handlers



---



# 8. Thymeleaf & Tailwind CSS Rules



## Frontend Stack



Preferred:



- Thymeleaf

- Tailwind CSS



Optional:



- Alpine.js

- HTMX



Avoid introducing React/Next.js CSR architecture unless explicitly required by the task or project direction.



---



## Tailwind CSS Rules



Prefer:



- utility-first styling

- responsive utilities

- semantic HTML

- flex layouts

- grid layouts

- consistent spacing systems



Use responsive prefixes:



- `sm:`

- `md:`

- `lg:`

- `xl:`

- `2xl:`



Prefer Tailwind design system consistency over excessive custom spacing and sizing.



Avoid:



- fixed-width layouts

- float-based layouts

- excessive arbitrary values

- unnecessary absolute positioning

- oversized SCSS architectures



---



## Responsive Design



Design mobile-first.



Requirements:



- no horizontal scrolling

- tablet compatibility

- desktop responsiveness

- accessible touch targets

- readable typography



Prefer:



```html

<img class="w-full h-auto object-cover">

```



Use fluid layouts and Tailwind spacing scales.



---



## Thymeleaf Rules



Use reusable fragments:



- header

- footer

- sidebar

- modal

- navigation



Use:



```html

th:fragment

th:replace

```



Avoid large inline template logic.



---



# 9. Accessibility Rules



## WCAG AA Compliance



Follow WCAG AA.



Requirements:



- semantic HTML

- keyboard accessibility

- visible focus states

- accessible labels

- sufficient contrast

- logical heading structure

- descriptive alt text



Every form input must include:



```html

<label>

```



Use ARIA only when necessary.



---



# 10. Logging Rules



## Logging Standards



Use:



```java

@Slf4j

```



Use structured logging.



Appropriate levels:



- `log.info()`

- `log.warn()`

- `log.error()`



Avoid excessive noisy logging in production paths.



Never use:



```java

System.out.println()

```



Never log:



- passwords

- tokens

- secrets

- sensitive personal information



---



# 11. API Rules



## REST API Standards



Use versioned API paths for REST APIs.



Example:



```text

/api/v1/users

```



Use proper HTTP methods:



- GET

- POST

- PUT

- PATCH

- DELETE



Use proper HTTP status codes.



---



# 12. Testing Rules



## Testing Standards



Use:



- JUnit 5

- Mockito

- Testcontainers



Follow AAA pattern:



```text

Arrange

Act

Assert

```



Use descriptive test names.



Correct:



```java

shouldReturn404WhenUserDoesNotExist

```



Incorrect:



```java

test1

```



Mock dependencies only.



Never mock the class under test.



---



# 13. Production Readiness Rules



## Production Stability



Provide endpoints:



- `/healthz`

- `/readyz`



Support:



- graceful shutdown

- timeout handling

- retry handling

- failure recovery



Fail loudly in development.



Recover safely in production.



---



# 14. AI Agent Workflow Rules



## Planning Before Implementation



Before multi-file modifications:



1. explain implementation plan

2. identify affected files

3. describe architecture impact

4. verify constraints



---



## Incremental Development



Requirements:



- make small incremental changes

- verify frequently

- preserve existing functionality

- avoid massive rewrites

- avoid unrelated refactors



---



## Verification Before Completion



Never mark tasks complete without:



- successful build

- regression verification

- log verification

- responsive UI verification

- WebSocket verification when applicable



---



## Task Documentation



Before implementation update:



```text

tasks/todo.md

```



After major fixes update:



```text

tasks/lessons.md

```



---



# 15. Engineering Principles



## Core Engineering Philosophy



Always prioritize:



- simplicity

- maintainability

- readability

- explicitness

- practical implementation

- stable delivery

- minimal safe changes



Avoid:



- overengineering

- premature optimization

- speculative abstractions

- unnecessary framework complexity

- deeply coupled modules

- enterprise-scale architecture without business need



Optimize only after measurement.



Prefer clarity over cleverness.