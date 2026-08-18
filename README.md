# FirstClub Membership Program

A Spring Boot backend for managing FirstClub memberships, tiers, benefits, subscriptions, orders, and tier eligibility.

## Tech Stack

- Java 17
- Spring Boot 3.3.2
- Spring Web
- Spring Data JPA
- H2
- Maven
- JUnit 5

## Features

- Monthly, Quarterly, and Yearly membership plans
- Silver, Gold, and Platinum tiers
- Configurable tier benefits and eligibility criteria
- Subscribe, upgrade, downgrade, and cancel membership
- Subscription history and expiry tracking
- Idempotent subscription operations
- Order-based tier evaluation
- Checkout benefit calculation
- Scheduled lifecycle and tier evaluation
- Admin APIs for updating tier configuration

## Project Structure

```text
src/main/java/com/firstclub/membership
├── api
├── benefit
├── bootstrap
├── catalog
├── common
├── config
├── domain
├── order
├── subscription
└── tier
```

Business logic is separated from controllers, with separate handlers/evaluators for benefits and tier criteria.

## Run Locally

Requirements: Java 17+ and Maven.

```bash
mvn clean package
mvn test
mvn spring-boot:run
```

Application:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

H2 Console:

```text
http://localhost:8080/h2-console
```

## APIs

Base path:

```text
/api/v1
```

Main APIs include:

```text
GET  /plans
GET  /tiers
POST /subscriptions
GET  /users/{userId}/membership
POST /subscriptions/{id}/upgrade
POST /subscriptions/{id}/downgrade
POST /subscriptions/{id}/cancel
POST /orders
POST /checkout/preview
GET  /users/{userId}/benefits
GET  /users/{userId}/tier-eligibility
```

Admin APIs are available for updating tier benefits/criteria and running lifecycle and tier-evaluation sweeps.

See `api-examples.http` for request examples.

## Demo

The repository also contains `demo.sh` to demonstrate the main membership flows.

## Database

The application uses an H2 in-memory database, so no external database setup is required.

Demo data can be controlled with:

```text
membership.seed-demo-data=true
```

## Tests

Tests are available under:

```text
src/test/java/com/firstclub/membership
```

Run them with:

```bash
mvn test
```
