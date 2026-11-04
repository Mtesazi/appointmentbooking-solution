import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/authentication/pages/login/login')
        .then(m => m.Login)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/authentication/pages/register/register')
        .then(m => m.Register)
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/customer/pages/dashboard/dashboard')
        .then(m => m.Dashboard)
  },
  {
    path: 'customer/book-appointment',
    loadComponent: () =>
      import('./features/appointments/pages/book-appointment/book-appointment')
        .then(m => m.BookAppointment)
  },
  {
    path: 'customer/booking-confirmation',
    loadComponent: () =>
      import('./features/appointments/pages/booking-confirmation/booking-confirmation')
        .then(m => m.BookingConfirmation)
  },
  {
    path: 'customer/my-appointments',
    loadComponent: () =>
      import('./features/appointments/pages/my-appointments/my-appointments')
        .then(m => m.MyAppointments)
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];