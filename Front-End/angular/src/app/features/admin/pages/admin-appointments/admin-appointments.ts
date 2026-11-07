import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AdminAppointment,
  AdminAppointmentStatus
} from '../../models/admin-appointment.model';
import { AdminDataService } from '../../services/admin-data.service';
import { getApiErrorMessage } from '../../../../core/api-error';

@Component({
  selector: 'app-admin-appointments',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './admin-appointments.html',
  styleUrl: './admin-appointments.scss'
})
export class AdminAppointments implements OnInit {
  private readonly adminData = inject(AdminDataService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  appointments: AdminAppointment[] = [];

  searchTerm = '';
  selectedStatus: 'ALL' | AdminAppointmentStatus = 'ALL';
  loading = true;
  errorMessage = '';
  cancellationError = '';
  successMessage = '';
  cancellingId: number | null = null;
  pendingCancellation: AdminAppointment | null = null;

  ngOnInit(): void {
    this.loadAppointments();
  }

  confirmCancellation(appointment: AdminAppointment): void {
    if (this.cancellingId !== null || appointment.status !== 'CONFIRMED') {
      return;
    }
    this.pendingCancellation = appointment;
    this.cancellationError = '';
    this.successMessage = '';
  }

  dismissCancellation(): void {
    if (this.cancellingId === null) {
      this.pendingCancellation = null;
    }
  }

  cancelAppointment(): void {
    const appointment = this.pendingCancellation;
    if (!appointment || this.cancellingId !== null) {
      return;
    }
    this.cancellingId = appointment.id;
    this.cancellationError = '';
    this.adminData.cancelAppointment(appointment.id).subscribe({
      next: () => {
        this.appointments = this.appointments.map(item =>
          item.id === appointment.id ? { ...item, status: 'CANCELLED' } : item
        );
        this.pendingCancellation = null;
        this.cancellingId = null;
        this.successMessage = `Appointment #${appointment.id} was cancelled.`;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.cancellingId = null;
        this.cancellationError = getApiErrorMessage(error);
        this.changeDetector.markForCheck();
      }
    });
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';
    this.pendingCancellation = null;
    this.cancellationError = '';
    this.adminData.getAppointments().subscribe({
      next: appointments => {
        this.appointments = appointments;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  get filteredAppointments(): AdminAppointment[] {
    const query = this.searchTerm.trim().toLowerCase();

    return this.appointments.filter(appointment => {
      const matchesStatus =
        this.selectedStatus === 'ALL' || appointment.status === this.selectedStatus;
      const matchesSearch =
        !query ||
        [
          appointment.customerName,
          appointment.customerEmail,
          appointment.service,
          appointment.branch,
          String(appointment.id)
        ].some(value => value.toLowerCase().includes(query));

      return matchesStatus && matchesSearch;
    });
  }
}
