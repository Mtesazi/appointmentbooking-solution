import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Branch } from '../branches/branch.model';
import {
  Appointment,
  AppointmentServiceOption,
  CreateAppointmentRequest,
  Dashboard
} from './appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}`;
  private readonly confirmationStorageKey = 'appointment-booking-last-confirmation';
  private cachedAppointments: Appointment[] | null = null;

  getBranches(): Observable<Branch[]> {
    return this.http.get<Branch[]>(`${this.apiUrl}/branches`);
  }

  getServices(): Observable<AppointmentServiceOption[]> {
    return this.http.get<AppointmentServiceOption[]>(`${this.apiUrl}/services`);
  }

  getAvailableTimes(branchId: number, serviceId: number, date: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/appointments/availability`, {
      params: { branchId, serviceId, date }
    });
  }

  getRescheduleAvailableTimes(id: number, date: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/appointments/${id}/availability`, { params: { date } });
  }

  rescheduleAppointment(id: number, appointmentDate: string, time: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apiUrl}/appointments/${id}/reschedule`, { appointmentDate, time })
      .pipe(tap(appointment => this.updateStoredAppointment(appointment)));
  }

  getAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apiUrl}/appointments`).pipe(
      timeout({ first: 15000 }),
      tap(appointments => {
        if (appointments.length > 0 || this.cachedAppointments === null) {
          this.cachedAppointments = appointments;
        }
      })
    );
  }

  getCachedAppointments(): Appointment[] | null {
    return this.cachedAppointments;
  }

  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apiUrl}/appointments`, request);
  }

  cancelAppointment(id: number): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apiUrl}/appointments/${id}/cancel`, {}).pipe(
      tap(appointment => this.updateStoredAppointment(appointment))
    );
  }

  private updateStoredAppointment(updated: Appointment): void {
    if (this.cachedAppointments) {
      this.cachedAppointments = this.cachedAppointments.map(appointment =>
        appointment.id === updated.id ? updated : appointment
      );
    }
    if (this.getBookingConfirmation()?.id === updated.id) {
      sessionStorage.setItem(this.confirmationStorageKey, JSON.stringify(updated));
    }
  }

  getDashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.apiUrl}/customer/dashboard`);
  }

  saveBookingConfirmation(appointment: Appointment): void {
    sessionStorage.setItem(this.confirmationStorageKey, JSON.stringify(appointment));
    this.cachedAppointments = [
      appointment,
      ...(this.cachedAppointments ?? []).filter(item => item.id !== appointment.id)
    ];
  }

  getBookingConfirmation(): Appointment | null {
    const stored = sessionStorage.getItem(this.confirmationStorageKey);
    return stored ? JSON.parse(stored) as Appointment : null;
  }
}
