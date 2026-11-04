export interface Appointment {
  id: number;
  branch: string;
  service: string;
  date: string;
  time: string;
  status: 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  notes: string | null;
}

export interface AppointmentServiceOption {
  id: number;
  name: string;
  durationMinutes: number;
}

export interface CreateAppointmentRequest {
  branchId: number;
  serviceId: number;
  appointmentDate: string;
  time: string;
  notes?: string;
}

export interface Dashboard {
  customerName: string;
  upcomingAppointments: number;
  completedAppointments: number;
  cancelledAppointments: number;
  nextAppointment: Appointment | null;
  recentAppointments: Appointment[];
}
