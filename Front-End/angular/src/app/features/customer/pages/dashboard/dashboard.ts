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

  ngOnInit(): void {
    this.appointmentService.getDashboard().subscribe({
      next: (dashboard: DashboardData) => {
        this.customerName = dashboard.customerName;
        this.upcomingAppointments = dashboard.upcomingAppointments;
        this.completedAppointments = dashboard.completedAppointments;
        this.cancelledAppointments = dashboard.cancelledAppointments;
        this.nextAppointment = dashboard.nextAppointment;
        this.recentAppointments = dashboard.recentAppointments;
        this.loading = false;
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
      }
    });
  }

  signOut(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
