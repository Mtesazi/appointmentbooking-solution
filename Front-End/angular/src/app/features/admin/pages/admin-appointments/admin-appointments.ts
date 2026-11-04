import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AdminAppointment,
  AdminAppointmentStatus
} from '../../models/admin-appointment.model';
import { AdminPreviewService } from '../../services/admin-preview.service';

@Component({
  selector: 'app-admin-appointments',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './admin-appointments.html',
  styleUrl: './admin-appointments.scss'
})
export class AdminAppointments {
  private readonly preview = inject(AdminPreviewService);
  private readonly appointments = this.preview.getAppointments();

  searchTerm = '';
  selectedStatus: 'ALL' | AdminAppointmentStatus = 'ALL';

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
