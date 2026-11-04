import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminPreviewService } from '../../services/admin-preview.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss'
})
export class AdminDashboard {
  private readonly preview = inject(AdminPreviewService);

  readonly stats = this.preview.getDashboardStats();
  readonly recentAppointments = this.preview.getAppointments().slice(0, 5);
}
