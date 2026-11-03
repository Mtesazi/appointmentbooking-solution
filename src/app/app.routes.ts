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
    path: '**',
    redirectTo: 'login'
  }
];