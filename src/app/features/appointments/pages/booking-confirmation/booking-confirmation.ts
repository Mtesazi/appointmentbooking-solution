import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AppointmentService } from '../../appointment.service';
import { Appointment } from '../../appointment.model';

@Component({
  selector: 'app-booking-confirmation',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './booking-confirmation.html',
  styleUrl: './booking-confirmation.scss'
})
export class BookingConfirmation {
  private readonly appointmentService = inject(AppointmentService);

  appointment: Appointment | null =
    this.appointmentService.getAppointments()[0] ?? null;
}
