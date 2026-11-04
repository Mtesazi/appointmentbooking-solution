import { Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AppointmentService } from '../../appointment.service';
import { Appointment } from '../../appointment.model';
import { getApiErrorMessage } from '../../../../core/api-error';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.scss'
})
export class MyAppointments implements OnInit {
  private readonly appointmentService = inject(AppointmentService);

  appointments: Appointment[] = [];
  loading = true;
  errorMessage = '';
  cancellingId: number | null = null;

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.loading = true;
    this.appointmentService.getAppointments().subscribe({
      next: appointments => {
        this.appointments = appointments;
        this.loading = false;
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
      }
    });
  }

  cancelAppointment(appointment: Appointment): void {
    if (appointment.status !== 'CONFIRMED' || this.cancellingId !== null) {
      return;
    }

    this.cancellingId = appointment.id;
    this.errorMessage = '';
    this.appointmentService.cancelAppointment(appointment.id).subscribe({
      next: updatedAppointment => {
        this.appointments = this.appointments.map(item =>
          item.id === updatedAppointment.id ? updatedAppointment : item
        );
        this.cancellingId = null;
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.cancellingId = null;
      }
    });
  }

  get upcomingAppointments(): Appointment[] {
    return this.appointments.filter(
      appointment => appointment.status === 'CONFIRMED'
    );
  }

  get pastAppointments(): Appointment[] {
    return this.appointments.filter(
      appointment => appointment.status !== 'CONFIRMED'
    );
  }
}
