# Appointment Booking

A modern Angular frontend for a smart appointment booking system. The application currently includes customer authentication flows, a dashboard overview, and a booking form for scheduling appointments.

## Overview

This frontend is built with Angular 22 and provides a clean customer portal experience with:

- Login page
- Registration page
- Customer dashboard
- Appointment booking form
- Route-based navigation for user flows

## Current Features

- Authentication pages under `src/app/features/authentication/pages/`
- Customer dashboard under `src/app/features/customer/pages/dashboard/`
- Booking flow under `src/app/features/appointments/pages/book-appointment/`
- Shared app layout and routing through `src/app/app.routes.ts`

## Project Structure

```text
src/app/
├── core/
│   ├── guards/
│   ├── interceptors/
│   ├── services/
│   └── models/
├── shared/
│   ├── components/
│   ├── directives/
│   └── pipes/
├── layouts/
│   ├── customer-layout/
│   └── admin-layout/
├── features/
│   ├── authentication/
│   │   └── pages/
│   │       ├── login/
│   │       └── register/
│   ├── customer/
│   ├── branches/
│   ├── appointments/
│   ├── notifications/
│   └── administration/
├── app.routes.ts
├── app.ts
├── app.html
├── app.scss
└── app.config.ts
```

## Routes

The application currently supports the following routes:

- `/login`
- `/register`
- `/dashboard`
- `/customer/book-appointment`

## Getting Started

### Install dependencies

```bash
npm install
```

### Run the app locally

```bash
ng serve
```

Then open:

```text
http://localhost:4200/
```

## Build

```bash
ng build
```

## Testing

```bash
ng test
```

## Notes

This frontend is structured for future backend integration. The current dashboard and booking screen use demo data and are ready to be connected to real API services once the backend is available.
