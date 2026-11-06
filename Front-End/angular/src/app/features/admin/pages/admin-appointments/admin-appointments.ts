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

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';
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
