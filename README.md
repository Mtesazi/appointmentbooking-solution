# Appointment Booking

Appointment Booking is a full-stack application for customers to register, sign in, browse services and branches, book appointments, and manage their bookings.

## Project structure

```text
.
├── Back-End/
│   ├── service-discovery/     Eureka service registry
│   ├── api-gateway/           Spring Cloud API Gateway
│   └── appointment-service/   Spring Boot REST API
└── Front-End/
    └── angular/               Angular customer application
```

## Features

- Customer registration and login with JWT authentication
- Customer profile updates for name and phone number
- Sign out with server-side access-token revocation
- Customer dashboard with appointment summaries
- Browse available branches and services
- Create, view, and cancel appointments
- Role-protected admin dashboard, appointment list, and branch management backed by PostgreSQL
- Per-branch weekly opening hours and schedule-aware booking availability
- Admin management of service names, durations and availability
- Eureka service discovery and Spring Cloud Gateway routing
- PostgreSQL persistence with Flyway-managed database migrations

## Requirements

- Java 21
- PostgreSQL 15 or compatible
- Node.js and npm compatible with the Angular version in `Front-End/angular/package.json`
- Docker Engine with the Docker Compose plugin (for the containerized setup)

The backend uses the included Maven Wrapper, so a separate Maven installation is not required.

## Run locally

### Run the full application with Docker Compose

Docker Compose starts PostgreSQL, service discovery, the appointment API, API gateway, Angular frontend, and a Mailpit test inbox. This setup is intended for local development and testing; Mailpit captures messages and does not deliver email to real customers.

Copy the example environment file and replace the example database password and JWT secret with local values:

```bash
cp .env.example .env
```

Set `POSTGRES_PASSWORD` and `JWT_SECRET` in `.env`. The JWT secret must be a random value of at least 32 bytes. Then build and start the application from the repository root:

```bash
docker compose up --build -d
```

Open the frontend at `http://localhost:4200`. The API is also available at `http://localhost:8080`, the appointment service at `http://localhost:8082`, and the Eureka dashboard at `http://localhost:8761`. Compose enables email notifications and routes them to Mailpit; view captured booking, rescheduling, and cancellation messages at `http://localhost:8026`.

The Compose setup maps PostgreSQL to host port `5433` and Mailpit to host ports `1026` (SMTP) and `8026` (web inbox) by default to avoid conflicts with common local services. Override these ports in `.env` if needed. The appointment service uses Mailpit over the private Compose network, so it does not depend on the host port mapping.

Ports `4200`, `8080`, `8082`, and `8761` must also be available; stop any locally running app services before starting Compose. Follow startup logs with `docker compose logs -f`; stop the containers with `docker compose down`. The PostgreSQL data volume is preserved when containers stop. The example credentials and Compose configuration are for local testing only; configure production secrets and a real SMTP provider separately.

### 1. Create the PostgreSQL database and application user

Start PostgreSQL, then create the database and a local user. On Ubuntu, open a PostgreSQL administrator session with `sudo -u postgres psql`. Replace the example password with a strong local development password:

```sql
CREATE USER "dev-user" WITH PASSWORD 'replace-with-a-local-password';
CREATE DATABASE appointment_booking OWNER "dev-user";
\q
```

Flyway creates the schema and seed catalog data from `Back-End/appointment-service/src/main/resources/db/migration` when the backend starts. The PostgreSQL setup starts with an empty database and does not import data from another database.

The booking overlap migration requires PostgreSQL's `btree_gist` extension and permission to create it. Existing overlapping confirmed appointments must be resolved before applying the migration; back up the database before upgrading.

### 2. Start service discovery and the gateway

Start the registry first, then the gateway, each in a separate terminal:

```bash
cd Back-End/service-discovery
../appointment-service/mvnw spring-boot:run
```

```bash
cd Back-End/api-gateway
../appointment-service/mvnw spring-boot:run
```

The Eureka dashboard is at `http://localhost:8761`. The gateway listens on `http://localhost:8080` and routes requests to the registered `appointment-service` instance.

The gateway uses `lb://appointment-service` by default; set `APPOINTMENT_SERVICE_URI` to override the route target when needed.

For `/api/**`, the gateway handles CORS for `http://localhost:4200` and `http://127.0.0.1:4200` and removes the `Origin` header before forwarding requests, so the service does not add duplicate CORS headers. The appointment service retains its own CORS configuration for direct local access; update both configurations if the frontend origin changes.

### 3. Configure and start the appointment API

The backend reads these environment variables:

| Variable | Required | Default / example |
|---|---|---|
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/appointment_booking` |
| `SPRING_DATASOURCE_USERNAME` | No | `dev-user` |
| `SPRING_DATASOURCE_PASSWORD` | Yes | The password configured for the PostgreSQL user |
| `JWT_SECRET` | Yes | A randomly generated secret of at least 32 bytes |
| `EUREKA_SERVER_URL` | No | `http://localhost:8761/eureka/` |

Set the variables in a third terminal and start the appointment API:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/appointment_booking'
export SPRING_DATASOURCE_USERNAME='dev-user'
export SPRING_DATASOURCE_PASSWORD='your-local-postgresql-password'
export JWT_SECRET='replace-with-a-random-secret-at-least-32-bytes-long'

cd Back-End/appointment-service
./mvnw spring-boot:run
```

Spring Boot does not load a `.env` file automatically when launched with Maven. Do not commit `.env` files or real credentials.

Appointment emails are queued in PostgreSQL in the same transaction as the booking change, then sent by the appointment service through SMTP. Booking, rescheduling, and customer- or admin-initiated cancellation send confirmation emails; reminders are not included. Email delivery is disabled by default for local development. To enable it in a deployment, configure these environment variables:

| Variable | Required when enabled | Example |
|---|---|---|
| `EMAIL_NOTIFICATIONS_ENABLED` | Yes | `true` |
| `EMAIL_FROM` | Yes | `appointments@example.com` |
| `MAIL_HOST` | Yes | SMTP provider hostname |
| `MAIL_PORT` | Yes | `587` |
| `MAIL_USERNAME` | Provider-dependent | SMTP account username |
| `MAIL_PASSWORD` | Provider-dependent | SMTP account password |
| `MAIL_SMTP_AUTH` | Provider-dependent | `true` |
| `MAIL_SMTP_STARTTLS` | Provider-dependent | `true` |

Keep SMTP credentials in the deployment secret manager. Failed sends are retried with exponential backoff; after ten failures a message is marked `DEAD` in `email_outbox` and requires operational review. The outbox provides durable, at-least-once delivery: a process interruption after the SMTP server accepts a message but before the database records success can result in a duplicate email.

The appointment service remains directly available on port `8082` for local diagnostics. Use `http://localhost:8080/api` for API requests through the gateway. Swagger UI is available through the gateway at `http://localhost:8080/swagger-ui/index.html`; the OpenAPI specification is at `http://localhost:8080/v3/api-docs`. In Swagger UI, use **Authorize** and enter a JWT access token to try authenticated endpoints. Obtain a token from the login or registration endpoint; admin endpoints still require an account with the `ADMIN` role.

### 4. Run the frontend

After configuring the database password and JWT secret above, you can start all services in order from the project root:

```bash
./run-local.sh
```

The script builds the backend applications, starts Eureka, the gateway, and appointment API in order, waits for service readiness and registration, then runs the Angular frontend. Stop it with `Ctrl+C`; it will stop the services it launched. Run `npm install` once in `Front-End/angular` if frontend dependencies are missing.

Open `http://localhost:4200`. The development frontend calls the API gateway at `http://localhost:8080/api` in `src/environments/environment.ts`.

The admin UI is available at `http://localhost:4200/admin`. Its dashboard, appointment list, and branch management use the backend database and require an account with the `ADMIN` role. New registrations always receive the `CUSTOMER` role.

To promote an existing account to admin, connect to the PostgreSQL database and update that account explicitly:

```sql
UPDATE customers SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

After the role migration, existing users should sign out and back in to receive a token with their role. An account promoted to admin must also sign in again before the admin UI is available. Do not expose a public admin-registration flow.

## API overview

| Method | Endpoint | Access |
|---|---|---|
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/auth/logout` | Authenticated |
| `GET` | `/api/auth/me` | Authenticated |
| `GET` | `/api/branches` | Authenticated |
| `GET` | `/api/services` | Authenticated |
| `GET` | `/api/customer/dashboard` | `CUSTOMER` |
| `GET`, `PUT` | `/api/customer/profile` | `CUSTOMER` |
| `GET` | `/api/appointments` | `CUSTOMER` |
| `GET` | `/api/appointments/availability?branchId=1&serviceId=1&date=YYYY-MM-DD` | `CUSTOMER` |
| `GET` | `/api/appointments/{appointmentId}/availability?date=YYYY-MM-DD` | `CUSTOMER` (owner) |
| `POST` | `/api/appointments` | `CUSTOMER` |
| `PATCH` | `/api/appointments/{appointmentId}/cancel` | `CUSTOMER` |
| `PATCH` | `/api/appointments/{appointmentId}/reschedule` | `CUSTOMER` (owner) |
| `GET` | `/api/admin/dashboard` | `ADMIN` |
| `GET` | `/api/admin/appointments` | `ADMIN` |
| `PATCH` | `/api/admin/appointments/{appointmentId}/cancel` | `ADMIN` |
| `GET`, `POST`, `PUT`, `DELETE` | `/api/admin/branches[/{id}]` | `ADMIN` |
| `GET`, `PUT` | `/api/admin/branches/{id}/hours` | `ADMIN` |
| `GET`, `POST` | `/api/admin/services` | `ADMIN` |
| `PUT` | `/api/admin/services/{id}` | `ADMIN` |

Availability uses the server's local time, the selected service duration, and confirmed bookings at the selected branch. A slot may be taken after it is displayed; booking is rechecked and PostgreSQL prevents overlapping confirmed appointments even for simultaneous requests. The booking page refreshes available times after a conflict.

Admins can configure one opening period per weekday or mark a day closed under **Branches → Opening hours**. Existing and new branches default to 09:00–17:00 every day. Availability offers starts every 30 minutes from opening time; bookings must finish by closing. Schedule changes that would leave a future confirmed appointment outside opening hours are rejected. Overnight hours and holiday exceptions are not supported. Times use the server's local time.

Admins can add or edit services under **Services**, including activating or deactivating them. Existing appointments keep their booked durations when a service changes; new bookings and rescheduling use the current duration. Inactive services cannot be newly booked, but existing appointments may still be rescheduled.

Customers can reschedule their own confirmed appointments before their scheduled start time from **My Appointments**. The branch and service remain unchanged; the availability and reschedule endpoints validate ownership and exclude the appointment itself from conflict checks. A reschedule request supplies `appointmentDate` and `time`.

Customers may cancel only their own confirmed appointments, and only before the scheduled start time, using the server's local time. At or after the start time the cancellation endpoint returns HTTP 409 and leaves the appointment unchanged.

Admins can cancel any customer's confirmed appointment before its scheduled start time from **Appointments**. The admin endpoint follows the same deadline and returns HTTP 204 on success.

Customers can update their first name, last name, and phone from **My Profile**. The profile endpoint uses the authenticated customer identity; email and role are not editable through it.

Authenticated requests must include the JWT access token in the `Authorization` header using the Bearer scheme.

## Tests and build

GitHub Actions runs the Angular tests and production build, gateway tests, discovery compilation, and all appointment-service tests against an isolated PostgreSQL 15 database on pushes and pull requests. The CI database and JWT values are disposable test-only settings; no production secrets are required.

To run all appointment-service tests locally, use a dedicated PostgreSQL test database. Its user must be able to create the `btree_gist` extension. Set the following values for that database (the JWT value below is for local tests only):

```bash
cd Back-End/appointment-service
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appointment_booking_test
export SPRING_DATASOURCE_USERNAME=appointment_test
export SPRING_DATASOURCE_PASSWORD=your_test_database_password
export JWT_SECRET=local-only-test-secret-at-least-32-bytes-long
export EUREKA_CLIENT_ENABLED=false
export SPRING_CLOUD_DISCOVERY_ENABLED=false
BOOKING_INTEGRATION_TEST=true ./mvnw test
```

The booking integration suite is opt-in via `BOOKING_INTEGRATION_TEST=true`. Without it, `./mvnw test` still requires the database and JWT configuration for the application context test. To run only the booking integration suite, add `-Dtest=BookingAvailabilityIntegrationTest` to the test command.

Gateway tests and discovery compilation (from the repository root):

```bash
(cd Back-End/api-gateway && ../appointment-service/mvnw test)
(cd Back-End/service-discovery && ../appointment-service/mvnw test)
```

Frontend tests and production build:

```bash
cd Front-End/angular
npm ci
npm test -- --watch=false
npm run build
```

## Configuration and deployment

The checked-in frontend environment and backend CORS configuration are set up for local development. Update them for your deployment environment, and provide database credentials and a strong JWT secret through your deployment platform's secret management. Never commit production secrets.
