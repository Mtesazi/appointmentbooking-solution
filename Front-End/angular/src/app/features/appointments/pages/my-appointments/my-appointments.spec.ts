import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { Appointment } from '../../appointment.model';
import { AppointmentService } from '../../appointment.service';
import { MyAppointments } from './my-appointments';

describe('MyAppointments rescheduling', () => {
  const appointment: Appointment = {
    id: 1, branch: 'Central', service: 'Consultation', date: '2099-01-01',
    time: '10:00 AM', status: 'CONFIRMED', notes: null
  };
  let first: Subject<string[]>;
  let second: Subject<string[]>;
  let refreshed: Subject<string[]>;
  let saving: Subject<Appointment>;
  const service = {
    getCachedAppointments: vi.fn(() => null),
    getAppointments: vi.fn(() => of([appointment])),
    getDashboard: vi.fn(),
    getRescheduleAvailableTimes: vi.fn(),
    rescheduleAppointment: vi.fn(),
    cancelAppointment: vi.fn()
  };

  beforeEach(async () => {
    first = new Subject<string[]>();
    second = new Subject<string[]>();
    refreshed = new Subject<string[]>();
    saving = new Subject<Appointment>();
    service.getRescheduleAvailableTimes.mockReset()
      .mockReturnValueOnce(first).mockReturnValueOnce(second).mockReturnValueOnce(refreshed);
    service.rescheduleAppointment.mockReset().mockReturnValue(saving);
    service.cancelAppointment.mockReset();
    await TestBed.configureTestingModule({
      imports: [MyAppointments],
      providers: [provideRouter([]), { provide: AppointmentService, useValue: service }]
    }).compileComponents();
  });

  it('ignores stale slots and refreshes on a conflict', () => {
    const fixture = TestBed.createComponent(MyAppointments);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.startReschedule(appointment);
    component.changeDate('2099-01-02');
    first.next(['9:00 AM']);
    second.next(['10:00 AM']);
    expect(component.availableTimes).toEqual(['10:00 AM']);
    component.rescheduleTime = '10:00 AM';
    component.submitReschedule();
    expect(service.rescheduleAppointment).toHaveBeenCalledWith(1, '2099-01-02', '10:00 AM');
    saving.error(new HttpErrorResponse({ status: 409, error: { message: 'Time unavailable' } }));
    expect(component.rescheduleTime).toBe('');
    expect(component.rescheduleError).toBe('Time unavailable');
    refreshed.next(['11:00 AM']);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('11:00 AM');
    expect(fixture.nativeElement.textContent).not.toContain('9:00 AM');
  });

  it('updates the displayed appointment after a successful reschedule', () => {
    const fixture = TestBed.createComponent(MyAppointments);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.startReschedule(appointment);
    first.next(['11:00 AM']);
    component.rescheduleTime = '11:00 AM';
    component.submitReschedule();
    saving.next({ ...appointment, time: '11:00 AM' });
    fixture.detectChanges();
    expect(component.rescheduling).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Your appointment has been rescheduled.');
    expect(fixture.nativeElement.textContent).toContain('11:00 AM');
  });

  it('shows cancellation feedback and moves the appointment to the past list', () => {
    service.cancelAppointment.mockReturnValue(of({ ...appointment, status: 'CANCELLED' }));
    const fixture = TestBed.createComponent(MyAppointments);
    fixture.detectChanges();
    fixture.componentInstance.cancelAppointment(appointment);
    fixture.detectChanges();
    expect(fixture.componentInstance.upcomingAppointments).toHaveLength(0);
    expect(fixture.componentInstance.pastAppointments[0].status).toBe('CANCELLED');
    expect(fixture.nativeElement.textContent).toContain('Your appointment has been cancelled.');
    expect(fixture.nativeElement.textContent).toContain('before their scheduled start time');
  });

  it('shows the server deadline error when cancellation is rejected', () => {
    const cancelled = new Subject<Appointment>();
    service.cancelAppointment.mockReturnValue(cancelled);
    const fixture = TestBed.createComponent(MyAppointments);
    fixture.detectChanges();
    fixture.componentInstance.cancelAppointment(appointment);
    cancelled.error(new HttpErrorResponse({ status: 409, error: { message: 'Appointment already started' } }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Appointment already started');
    expect(fixture.componentInstance.upcomingAppointments).toHaveLength(1);
  });
});
