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
    path: '**',
    redirectTo: 'login'
  }
];