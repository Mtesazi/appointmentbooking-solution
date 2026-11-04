export interface Appointment {
  id: string;
  branch: string;
  service: string;
  date: string;
  time: string;
  status: 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
}
