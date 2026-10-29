<<<<<<< HEAD
# Symposium Management System

A Java 21 / Spring Boot conference lifecycle platform for participants, organizers, administrators, and reviewers.

## Current stack

- Java 21
- Spring Boot 3.5.6
- Spring MVC + REST controllers
- Spring Data JPA
- Spring Security with HTTP Basic authentication
- H2 database for local development (PostgreSQL-ready via environment variables)
- Static frontend served from src/main/resources/static

## Included features

- Published conference listing and detail rendering
- Backend-authoritative registration eligibility and available-seat counts on event APIs
- Event search and category filtering on the landing page
- Registration endpoint with duplicate prevention and capacity checks
- Status validation for registrations, published events, and closed registration windows
- Organizer/admin dashboard summary endpoint for operational metrics
- Paper submission and review workflow endpoints
- Demo authentication for admin, organizer, reviewer, and participant accounts

## Local run

Install Maven 3.9+ and run:

```powershell
mvn spring-boot:run
```

Open http://localhost:8080.

## Demo credentials

Demo users use HTTP Basic authentication for protected APIs:

- admin / admin123
- organizer / organizer123
- reviewer / reviewer123
- participant / participant123

## Database configuration

The app defaults to an in-memory H2 database for zero-setup local development:

- JDBC URL: jdbc:h2:mem:symposium
- Username: sa
- Password: empty

To switch to PostgreSQL, set the following environment variables before starting the app:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/symposium"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your_password"
$env:DB_DRIVER = "org.postgresql.Driver"
```

## Validation and testing

Run the backend test suite with:

```powershell
mvn test
```

The project includes regression coverage for duplicate registration blocking and full-capacity rejection.

## Registration availability

The event API (`GET /api/events` and `GET /api/events/{id}`) returns
`registeredCount`, `availableSeats`, `registrationOpen`, `registrationStatus`, and
`registrationMessage`. The landing page uses these fields for featured events,
cards, details, and registration controls; eligibility is always rechecked by the
backend when a registration is submitted.

Registration is available only for published events whose deadline has not passed,
that have not ended or been cancelled, and that have remaining capacity. Date-only
deadlines and event end dates are inclusive in the `Asia/Kolkata` time zone.
Cancelled registrations do not consume capacity, and registration failures return
a structured JSON response with an appropriate non-2xx status.
=======
# symposium-management-system
>>>>>>> 7a37122bfc8460fff5432697d81490a7b0a68659
