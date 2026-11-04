import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

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
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/pages/dashboard/dashboard')
        .then(m => m.Dashboard)
  },
  {
    path: 'customer/book-appointment',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/appointments/pages/book-appointment/book-appointment')
        .then(m => m.BookAppointment)
  },
  {
    path: 'customer/booking-confirmation',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/appointments/pages/booking-confirmation/booking-confirmation')
        .then(m => m.BookingConfirmation)
  },
  {
    path: 'customer/my-appointments',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/appointments/pages/my-appointments/my-appointments')
        .then(m => m.MyAppointments)
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];