import { Injectable } from '@angular/core';
import { Appointment } from './appointment.model';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private readonly storageKey = 'appointpro-appointments';

  getAppointments(): Appointment[] {
    const stored = sessionStorage.getItem(this.storageKey);
    return stored ? JSON.parse(stored) as Appointment[] : [];
  }

  saveAppointment(appointment: Appointment): void {
    const appointments = this.getAppointments();
    appointments.unshift(appointment);

    sessionStorage.setItem(
      this.storageKey,
      JSON.stringify(appointments)
    );
  }
}
