import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthResponse } from './auth.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const response: AuthResponse = {
    accessToken: 'test-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    customer: { id: 1, firstName: 'Alex', lastName: 'Example', email: 'alex@example.com', phone: '1234567', role: 'CUSTOMER' }
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('stores the first name after login and removes it on logout', () => {
    service.login({ email: response.customer.email, password: 'password' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(response);
    expect(service.firstName).toBe('Alex');

    service.logout().subscribe();
    http.expectOne(`${environment.apiUrl}/auth/logout`).flush(null);
    expect(service.firstName).toBe('');
    expect(sessionStorage.getItem('appointment-booking-first-name')).toBeNull();
  });

  it('stores the first name after registration', () => {
    service.register({ firstName: 'Alex', lastName: 'Example', email: response.customer.email,
      phone: '1234567', password: 'password' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/register`).flush(response);
    expect(service.firstName).toBe('Alex');
  });

  it('does not expose a stored name without an authenticated session', () => {
    sessionStorage.setItem('appointment-booking-first-name', 'Old name');
    expect(service.firstName).toBe('');
  });

  it('updates the stored first name only after a successful profile save', () => {
    service.login({ email: response.customer.email, password: 'password' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(response);
    service.updateProfile({ firstName: 'New', lastName: 'Example', phone: '1234567' }).subscribe();
    expect(service.firstName).toBe('Alex');
    http.expectOne(`${environment.apiUrl}/customer/profile`)
      .flush({ ...response.customer, firstName: 'New' });
    expect(service.firstName).toBe('New');
  });
});
