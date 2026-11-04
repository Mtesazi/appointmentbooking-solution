# Appointment Booking Frontend

Angular customer application for the Appointment Booking system. It provides registration and login, a customer dashboard, service and branch selection, appointment booking, and appointment management.

For full-stack setup instructions, including MySQL and the Spring Boot backend, see the [project README](../../README.md).

## Run locally

```bash
npm install
npm start
```

Open `http://localhost:4200`. The development API URL is configured in `src/environments/environment.ts` and defaults to `http://localhost:8082/api`.

The admin UI preview is available at `http://localhost:4200/admin` and currently uses sample data. It includes a dashboard, appointments list, and branch management. Branch changes are held in memory and reset when the app reloads. Admin authentication, authorization, and live management APIs are planned for backend integration.

## Test and build

```bash
npm test
npm run build
```
