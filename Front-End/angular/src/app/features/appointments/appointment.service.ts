import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Branch } from '../branches/branch.model';
import {
  Appointment,
  AppointmentServiceOption,
  Branch,
  CreateAppointmentRequest,
  Dashboard
} from './appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}`;
  private readonly confirmationStorageKey = 'appointment-booking-last-confirmation';

  getBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${this.apiUrl}/branches`);
  }

  getServices(): Observable<AppointmentServiceOption[]> {
    return this.http.get<AppointmentServiceOption[]>(`${this.apiUrl}/services`);
  }

  getAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apiUrl}/appointments`);
  }

  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apiUrl}/appointments`, request);
  }

  cancelAppointment(id: number): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apiUrl}/appointments/${id}/cancel`, {});
  }

  getDashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.apiUrl}/customer/dashboard`);
  }

  saveBookingConfirmation(appointment: Appointment): void {
    sessionStorage.setItem(this.confirmationStorageKey, JSON.stringify(appointment));
  }

  getBookingConfirmation(): Appointment | null {
    const stored = sessionStorage.getItem(this.confirmationStorageKey);
    return stored ? JSON.parse(stored) as Appointment : null;
  }
}
