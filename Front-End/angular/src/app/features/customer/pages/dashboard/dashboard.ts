import { Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AppointmentService } from '../../../appointments/appointment.service';
import { Appointment, Dashboard as DashboardData } from '../../../appointments/appointment.model';
import { getApiErrorMessage } from '../../../../core/api-error';
import { AuthService } from '../../../../features/authentication/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private readonly appointmentService = inject(AppointmentService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  customerName = '';
  upcomingAppointments = 0;
  completedAppointments = 0;
  cancelledAppointments = 0;
  nextAppointment: Appointment | null = null;
  recentAppointments: Appointment[] = [];
  loading = true;
  errorMessage = '';
  appointmentsLoading = true;
  appointmentsErrorMessage = '';

  ngOnInit(): void {
    this.loadAppointments();

    this.appointmentService.getDashboard().subscribe({
      next: (dashboard: DashboardData) => {
        this.customerName = dashboard.customerName;
        this.upcomingAppointments = dashboard.upcomingAppointments;
        this.completedAppointments = dashboard.completedAppointments;
        this.cancelledAppointments = dashboard.cancelledAppointments;
        this.nextAppointment = dashboard.nextAppointment;
        this.loading = false;
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
      }
    });
  }

  loadAppointments(): void {
    this.appointmentsLoading = true;
    this.appointmentsErrorMessage = '';
    this.appointmentService.getAppointments().subscribe({
      next: appointments => {
        this.recentAppointments = appointments;
        this.appointmentsLoading = false;
      },
      error: error => {
        this.appointmentsErrorMessage = getApiErrorMessage(error);
        this.appointmentsLoading = false;
      }
    });
  }

  signOut(): void {
    this.auth.logout().subscribe({
      complete: () => void this.router.navigate(['/login']),
      error: () => void this.router.navigate(['/login'])
    });
  }
}
