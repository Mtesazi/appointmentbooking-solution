import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';

interface Appointment {
  service: string;
  branch: string;
  date: string;
  time: string;
  status: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard {
  customerName = 'John';

  upcomingAppointments = 2;
  completedAppointments = 8;
  cancelledAppointments = 1;

  nextAppointment: Appointment = {
    service: 'General Consultation',
    branch: 'Cape Town Central',
    date: '2026-11-06T00:00:00',
    time: '10:30 AM',
    status: 'CONFIRMED'
  };

  recentAppointments: Appointment[] = [
    {
      service: 'General Consultation',
      branch: 'Cape Town Central',
      date: '2026-10-28T00:00:00',
      time: '09:00 AM',
      status: 'COMPLETED'
    },
    {
      service: 'Follow-up Consultation',
      branch: 'Bellville Branch',
      date: '2026-10-20T00:00:00',
      time: '11:30 AM',
      status: 'COMPLETED'
    },
    {
      service: 'General Consultation',
      branch: 'Cape Town Central',
      date: '2026-10-15T00:00:00',
      time: '02:00 PM',
      status: 'CANCELLED'
    }
  ];
}
