import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../features/authentication/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated
    ? true
    : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export function roleGuard(requiredRole: 'CUSTOMER' | 'ADMIN'): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.role === requiredRole) {
      return true;
    }

    if (auth.role === 'ADMIN') {
      return router.createUrlTree(['/admin/dashboard']);
    }

    if (auth.role === 'CUSTOMER') {
      return router.createUrlTree(['/dashboard']);
    }

    return router.createUrlTree(['/login']);
  };
}
