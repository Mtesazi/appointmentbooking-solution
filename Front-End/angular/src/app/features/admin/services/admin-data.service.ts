import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AdminAppointment, AdminDashboardStats } from '../models/admin-appointment.model';
import { AdminBranch } from '../../branches/branch.model';

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

  getBranches(): Observable<AdminBranch[]> {
    return this.http.get<AdminBranch[]>(`${this.apiUrl}/branches`);
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
