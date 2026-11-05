# Appointment Booking

Appointment Booking is a full-stack application for customers to register, sign in, browse services and branches, book appointments, and manage their bookings.

## Project structure

```text
.
├── Back-End/
│   └── appointment-service/   Spring Boot REST API
└── Front-End/
    └── angular/               Angular customer application
```

## Features

- Customer registration and login with JWT authentication
- Customer dashboard with appointment summaries
- Browse available branches and services
- Create, view, and cancel appointments
- Admin dashboard and searchable appointment list (frontend preview with sample data)
- PostgreSQL persistence with Flyway-managed database migrations

## Requirements

- Java 21
- PostgreSQL 15 or compatible
- Node.js and npm compatible with the Angular version in `Front-End/angular/package.json`

The backend uses the included Maven Wrapper, so a separate Maven installation is not required.

## Run locally

### 1. Create the PostgreSQL database and application user

Start PostgreSQL, then create the database and a local user. On Ubuntu, open a PostgreSQL administrator session with `sudo -u postgres psql`. Replace the example password with a strong local development password:

```sql
CREATE USER "dev-user" WITH PASSWORD 'replace-with-a-local-password';
CREATE DATABASE appointment_booking OWNER "dev-user";
\q
```

Flyway creates the schema and seed catalog data from `Back-End/appointment-service/src/main/resources/db/migration` when the backend starts. The PostgreSQL setup starts with an empty database and does not import data from another database.

### 2. Configure and run the backend

The backend reads these environment variables:

| Variable | Required | Default / example |
|---|---|---|
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/appointment_booking` |
| `SPRING_DATASOURCE_USERNAME` | No | `dev-user` |
| `SPRING_DATASOURCE_PASSWORD` | Yes | The password configured for the PostgreSQL user |
| `JWT_SECRET` | Yes | A randomly generated secret of at least 32 bytes |

Set the variables in your shell or IDE run configuration. For example, in a Bash terminal:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/appointment_booking'
export SPRING_DATASOURCE_USERNAME='dev-user'
export SPRING_DATASOURCE_PASSWORD='your-local-postgresql-password'
export JWT_SECRET='replace-with-a-random-secret-at-least-32-bytes-long'

cd Back-End/appointment-service
./mvnw spring-boot:run
```

Spring Boot does not load a `.env` file automatically when launched with Maven. Do not commit `.env` files or real credentials.

The API runs at `http://localhost:8082/api`. The health endpoint is `http://localhost:8082/actuator/health`.

### 3. Run the frontend

In another terminal:

```bash
cd Front-End/angular
npm install
npm start
```

Open `http://localhost:4200`. The development frontend is configured to call the backend at `http://localhost:8082/api` in `src/environments/environment.ts`.

The admin UI preview is available at `http://localhost:4200/admin`. It includes a dashboard, appointments list, and branch management. Branches can be added, edited, searched, filtered, and removed in the preview; these changes are held in memory and reset when the app reloads. Admin authentication, authorization, and live administration APIs are not implemented yet.

## API overview

| Method | Endpoint | Access |
|---|---|---|
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `GET` | `/api/auth/me` | Authenticated |
| `GET` | `/api/branches` | Authenticated |
| `GET` | `/api/services` | Authenticated |
| `GET` | `/api/customer/dashboard` | Authenticated |
| `GET` | `/api/appointments` | Authenticated |
| `POST` | `/api/appointments` | Authenticated |
| `PATCH` | `/api/appointments/{appointmentId}/cancel` | Authenticated |

Authenticated requests must include the JWT access token in the `Authorization` header using the Bearer scheme.

## Tests and build

Backend tests:

```bash
cd Back-End/appointment-service
./mvnw test
```

Frontend tests and production build:

```bash
cd Front-End/angular
npm test
npm run build
```

## Configuration and deployment

The checked-in frontend environment and backend CORS configuration are set up for local development. Update them for your deployment environment, and provide database credentials and a strong JWT secret through your deployment platform's secret management. Never commit production secrets.
