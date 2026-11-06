import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';

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
    canActivate: [authGuard, roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/customer/pages/dashboard/dashboard')
        .then(m => m.Dashboard)
  },
  {
    path: 'customer/book-appointment',
    canActivate: [authGuard, roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/appointments/pages/book-appointment/book-appointment')
        .then(m => m.BookAppointment)
  },
  {
    path: 'customer/booking-confirmation',
    canActivate: [authGuard, roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/appointments/pages/booking-confirmation/booking-confirmation')
        .then(m => m.BookingConfirmation)
  },
  {
    path: 'customer/my-appointments',
    canActivate: [authGuard, roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/appointments/pages/my-appointments/my-appointments')
        .then(m => m.MyAppointments)
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./features/admin/layout/admin-layout/admin-layout')
        .then(m => m.AdminLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/admin/pages/admin-dashboard/admin-dashboard')
            .then(m => m.AdminDashboard)
      },
      {
        path: 'appointments',
        loadComponent: () =>
          import('./features/admin/pages/admin-appointments/admin-appointments')
            .then(m => m.AdminAppointments)
      },
      {
        path: 'branches',
        loadComponent: () =>
          import('./features/branches/pages/admin-branches/admin-branches')
            .then(m => m.AdminBranches)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];