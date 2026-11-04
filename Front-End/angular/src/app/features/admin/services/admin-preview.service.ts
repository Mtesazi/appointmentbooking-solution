import { Injectable } from '@angular/core';
import {
  AdminAppointment,
  AdminDashboardStats
} from '../models/admin-appointment.model';

@Injectable({ providedIn: 'root' })
export class AdminPreviewService {
  private readonly appointments: AdminAppointment[] = [
    {
      id: 1048,
      customerName: 'Amara Johnson',
      customerEmail: 'amara.johnson@example.com',
      service: 'Consultation',
      branch: 'Central Branch',
      date: '2026-11-05',
      time: '09:30',
      status: 'CONFIRMED'
    },
    {
      id: 1047,
      customerName: 'Noah Williams',
      customerEmail: 'noah.williams@example.com',
      service: 'Follow-up Visit',
      branch: 'North Branch',
      date: '2026-11-05',
      time: '11:00',
      status: 'CONFIRMED'
    },
    {
      id: 1046,
      customerName: 'Lerato Mokoena',
      customerEmail: 'lerato.mokoena@example.com',
      service: 'Initial Assessment',
      branch: 'Central Branch',
      date: '2026-11-06',
      time: '10:15',
      status: 'CONFIRMED'
    },
    {
      id: 1045,
      customerName: 'Ethan Davis',
      customerEmail: 'ethan.davis@example.com',
      service: 'Consultation',
      branch: 'South Branch',
      date: '2026-11-02',
      time: '14:00',
      status: 'COMPLETED'
    },
    {
      id: 1044,
      customerName: 'Zanele Khumalo',
      customerEmail: 'zanele.khumalo@example.com',
      service: 'Follow-up Visit',
      branch: 'North Branch',
      date: '2026-11-01',
      time: '08:45',
      status: 'CANCELLED'
    },
    {
      id: 1043,
      customerName: 'Olivia Brown',
      customerEmail: 'olivia.brown@example.com',
      service: 'Initial Assessment',
      branch: 'South Branch',
      date: '2026-10-30',
      time: '12:30',
      status: 'COMPLETED'
    },
    {
      id: 1042,
      customerName: 'Thabo Nkosi',
      customerEmail: 'thabo.nkosi@example.com',
      service: 'Consultation',
      branch: 'Central Branch',
      date: '2026-10-29',
      time: '15:15',
      status: 'COMPLETED'
    }
  ];

  getAppointments(): AdminAppointment[] {
    return this.appointments;
  }

  getDashboardStats(): AdminDashboardStats {
    return {
      totalAppointments: 248,
      upcomingAppointments: this.appointments.filter(
        appointment => appointment.status === 'CONFIRMED'
      ).length,
      totalCustomers: 126,
      activeServices: 8
    };
  }
}
