import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminDataService } from '../../services/admin-data.service';
import { AdminAppointment, AdminDashboardStats } from '../../models/admin-appointment.model';
import { getApiErrorMessage } from '../../../../core/api-error';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss'
})
export class AdminDashboard implements OnInit {
  private readonly adminData = inject(AdminDataService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  stats: AdminDashboardStats | null = null;
  recentAppointments: AdminAppointment[] = [];
  loading = true;
  errorMessage = '';
  appointmentsLoading = true;
  appointmentsErrorMessage = '';

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminData.getDashboard().subscribe({
      next: dashboard => {
        this.stats = dashboard;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
        this.changeDetector.markForCheck();
      }
    });
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.appointmentsLoading = true;
    this.appointmentsErrorMessage = '';
    this.adminData.getAppointments().subscribe({
      next: appointments => {
        this.recentAppointments = appointments.slice(0, 5);
        this.appointmentsLoading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.appointmentsErrorMessage = getApiErrorMessage(error);
        this.appointmentsLoading = false;
        this.changeDetector.markForCheck();
      }
    });
  }
}
