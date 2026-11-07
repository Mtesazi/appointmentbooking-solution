import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

const tokenStorageKey = 'appointment-booking-access-token';
const firstNameStorageKey = 'appointment-booking-first-name';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = sessionStorage.getItem(tokenStorageKey);

  if (!token || !request.url.startsWith(environment.apiUrl)
      || request.url.endsWith('/auth/login') || request.url.endsWith('/auth/register')) {
    return next(request);
  }

  const router = inject(Router);
  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  })).pipe(catchError(error => {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      sessionStorage.removeItem(tokenStorageKey);
      sessionStorage.removeItem(firstNameStorageKey);
      void router.navigate(['/login']);
    }

    return throwError(() => error);
  }));
};
