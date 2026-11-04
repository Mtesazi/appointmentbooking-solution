import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from './auth.model';

const tokenStorageKey = 'appointment-booking-access-token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authUrl = `${environment.apiUrl}/auth`;

  get isAuthenticated(): boolean {
    return sessionStorage.getItem(tokenStorageKey) !== null;
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, request)
      .pipe(tap(response => this.storeToken(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authUrl}/register`, request)
      .pipe(tap(response => this.storeToken(response)));
  }

  logout(): void {
    sessionStorage.removeItem(tokenStorageKey);
  }

  private storeToken(response: AuthResponse): void {
    sessionStorage.setItem(tokenStorageKey, response.accessToken);
  }
}
