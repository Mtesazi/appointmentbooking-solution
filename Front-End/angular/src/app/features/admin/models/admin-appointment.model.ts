export type AdminAppointmentStatus = 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface AdminAppointment {
  id: number;
  customerName: string;
  customerEmail: string;
  service: string;
  branch: string;
  date: string;
  time: string;
  status: AdminAppointmentStatus;
}

export interface AdminDashboardStats {
  totalAppointments: number;
  upcomingAppointments: number;
  totalCustomers: number;
  activeServices: number;
}
