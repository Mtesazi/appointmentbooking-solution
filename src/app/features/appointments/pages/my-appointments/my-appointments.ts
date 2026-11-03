import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AppointmentService } from '../../appointment.service';
import { Appointment } from '../../appointment.model';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.scss'
})
export class MyAppointments {
  private readonly appointmentService = inject(AppointmentService);

  appointments: Appointment[] =
    this.appointmentService.getAppointments();

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
