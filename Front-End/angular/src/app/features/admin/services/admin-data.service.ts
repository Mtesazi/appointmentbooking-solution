import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AdminAppointment, AdminDashboardStats } from '../models/admin-appointment.model';
import { AdminBranch } from '../../branches/branch.model';

export interface BranchDayHours {
  dayOfWeek: number;
  closed: boolean;
  openTime: string | null;
  closeTime: string | null;
}

export interface AdminServiceOption {
  id: number;
  name: string;
  durationMinutes: number;
  active: boolean;
}

export type SaveServiceOption = Omit<AdminServiceOption, 'id'>;

@Injectable({ providedIn: 'root' })
export class AdminDataService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin`;

  getDashboard(): Observable<AdminDashboardStats> {
    return this.http.get<AdminDashboardStats>(`${this.apiUrl}/dashboard`);
  }

  getAppointments(): Observable<AdminAppointment[]> {
    return this.http.get<AdminAppointment[]>(`${this.apiUrl}/appointments`);
  }

  cancelAppointment(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/appointments/${id}/cancel`, {});
  }

  getServices(): Observable<AdminServiceOption[]> {
    return this.http.get<AdminServiceOption[]>(`${this.apiUrl}/services`);
  }

  createService(service: SaveServiceOption): Observable<AdminServiceOption> {
    return this.http.post<AdminServiceOption>(`${this.apiUrl}/services`, service);
  }

  updateService(id: number, service: SaveServiceOption): Observable<AdminServiceOption> {
    return this.http.put<AdminServiceOption>(`${this.apiUrl}/services/${id}`, service);
  }

  getBranches(): Observable<AdminBranch[]> {
    return this.http.get<AdminBranch[]>(`${this.apiUrl}/branches`);
  }

  getBranchHours(id: number): Observable<BranchDayHours[]> {
    return this.http.get<BranchDayHours[]>(`${this.apiUrl}/branches/${id}/hours`);
  }

  saveBranchHours(id: number, days: BranchDayHours[]): Observable<BranchDayHours[]> {
    return this.http.put<BranchDayHours[]>(`${this.apiUrl}/branches/${id}/hours`, { days });
  }

  createBranch(branch: Pick<AdminBranch, 'name' | 'active'>): Observable<AdminBranch> {
    return this.http.post<AdminBranch>(`${this.apiUrl}/branches`, branch);
  }

  updateBranch(id: number, branch: Pick<AdminBranch, 'name' | 'active'>): Observable<AdminBranch> {
    return this.http.put<AdminBranch>(`${this.apiUrl}/branches/${id}`, branch);
  }

  deactivateBranch(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/branches/${id}`);
  }
}
