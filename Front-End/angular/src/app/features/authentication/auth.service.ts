import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, finalize, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, Customer, LoginRequest, RegisterRequest, UpdateProfileRequest } from './auth.model';

const tokenStorageKey = 'appointment-booking-access-token';
const roleStorageKey = 'appointment-booking-user-role';
const firstNameStorageKey = 'appointment-booking-first-name';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authUrl = `${environment.apiUrl}/auth`;

  get isAuthenticated(): boolean {
    return sessionStorage.getItem(tokenStorageKey) !== null;
  }

  get role(): 'CUSTOMER' | 'ADMIN' | null {
    const role = sessionStorage.getItem(roleStorageKey);
    return role === 'CUSTOMER' || role === 'ADMIN' ? role : null;
  }

  get isAdmin(): boolean {
    return this.role === 'ADMIN';
  }

  get firstName(): string {
    return this.isAuthenticated ? sessionStorage.getItem(firstNameStorageKey) ?? '' : '';
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, request)
      .pipe(tap(response => this.storeToken(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/register`, request)
      .pipe(tap(response => this.storeToken(response)));
  }

  getProfile(): Observable<Customer> {
    return this.http.get<Customer>(`${environment.apiUrl}/customer/profile`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<Customer> {
    return this.http.put<Customer>(`${environment.apiUrl}/customer/profile`, request).pipe(
      tap(customer => sessionStorage.setItem(firstNameStorageKey, customer.firstName))
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.authUrl}/logout`, {}).pipe(
      finalize(() => {
        sessionStorage.removeItem(tokenStorageKey);
        sessionStorage.removeItem(roleStorageKey);
        sessionStorage.removeItem(firstNameStorageKey);
      })
    );
  }

  private storeToken(response: AuthResponse): void {
    sessionStorage.setItem(tokenStorageKey, response.accessToken);
    sessionStorage.setItem(roleStorageKey, response.customer.role);
    sessionStorage.setItem(firstNameStorageKey, response.customer.firstName);
  }
}
