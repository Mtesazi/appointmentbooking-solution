import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AdminAppointment } from '../../models/admin-appointment.model';
import { AdminDataService } from '../../services/admin-data.service';
import { AdminAppointments } from './admin-appointments';

describe('AdminAppointments cancellation', () => {
  const appointment: AdminAppointment = {
    id: 1, customerName: 'Customer Example', customerEmail: 'customer@example.invalid',
    branch: 'Central', service: 'Consultation', date: '2099-01-01', time: '10:00 AM', status: 'CONFIRMED'
  };
  let cancellation: Subject<void>;
  const service = {
    getAppointments: vi.fn(() => of([appointment])),
    cancelAppointment: vi.fn()
  };

  beforeEach(async () => {
    cancellation = new Subject<void>();
    service.cancelAppointment.mockReset().mockReturnValue(cancellation);
    await TestBed.configureTestingModule({
      imports: [AdminAppointments],
      providers: [{ provide: AdminDataService, useValue: service }]
    }).compileComponents();
  });

  it('requires confirmation and updates the appointment after success', () => {
    const fixture = TestBed.createComponent(AdminAppointments);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.confirmCancellation(appointment);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Customer Example');
    expect(service.cancelAppointment).not.toHaveBeenCalled();
    component.cancelAppointment();
    expect(service.cancelAppointment).toHaveBeenCalledWith(1);
    cancellation.next();
    cancellation.complete();
    fixture.detectChanges();
    expect(component.appointments[0].status).toBe('CANCELLED');
    expect(fixture.nativeElement.textContent).toContain('Appointment #1 was cancelled.');
    expect(fixture.nativeElement.querySelector('[aria-label="Cancel appointment 1"]')).toBeNull();
  });

  it('retains the appointment and shows rejection errors', () => {
    const fixture = TestBed.createComponent(AdminAppointments);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.confirmCancellation(appointment);
    component.cancelAppointment();
    cancellation.error(new HttpErrorResponse({ status: 409, error: { message: 'Appointment already started' } }));
    fixture.detectChanges();
    expect(component.appointments[0].status).toBe('CONFIRMED');
    expect(fixture.nativeElement.textContent).toContain('Appointment already started');
  });
});
