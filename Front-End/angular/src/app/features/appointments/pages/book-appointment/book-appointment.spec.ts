import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AppointmentService } from '../../appointment.service';
import { BookAppointment } from './book-appointment';

describe('BookAppointment availability', () => {
  const firstTimes = new Subject<string[]>();
  const secondTimes = new Subject<string[]>();
  const refreshedTimes = new Subject<string[]>();
  const booking = new Subject<never>();
  const service = {
    getBranches: vi.fn(() => of([{ id: 1, name: 'Branch', active: true }])),
    getServices: vi.fn(() => of([{ id: 1, name: 'Service', durationMinutes: 30 }])),
    getAvailableTimes: vi.fn(),
    createAppointment: vi.fn(() => booking.asObservable()),
    saveBookingConfirmation: vi.fn()
  };

  beforeEach(async () => {
    service.getAvailableTimes.mockReset();
    service.createAppointment.mockClear();
    await TestBed.configureTestingModule({
      imports: [BookAppointment],
      providers: [provideRouter([]), { provide: AppointmentService, useValue: service }]
    }).compileComponents();
  });

  it('ignores stale availability and refreshes after a booking conflict', () => {
    service.getAvailableTimes
      .mockReturnValueOnce(firstTimes)
      .mockReturnValueOnce(secondTimes)
      .mockReturnValueOnce(refreshedTimes);
    const fixture = TestBed.createComponent(BookAppointment);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.bookingForm.setValue({ branchId: '1', serviceId: '1', appointmentDate: '2099-01-01' });
    component.bookingForm.controls.appointmentDate.setValue('2099-01-02');
    firstTimes.next(['9:00 AM']);
    secondTimes.next(['10:00 AM']);
    expect(component.timeSlots).toEqual(['10:00 AM']);
    component.selectTime('10:00 AM');
    component.onSubmit();
    booking.error(new HttpErrorResponse({ status: 409, error: { message: 'Time unavailable' } }));
    expect(component.selectedTime).toBe('');
    expect(component.errorMessage).toBe('Time unavailable');
    expect(component.loadingAvailability).toBe(true);
    refreshedTimes.next(['11:00 AM']);
    fixture.detectChanges();
    expect(component.timeSlots).toEqual(['11:00 AM']);
    expect(fixture.nativeElement.textContent).toContain('11:00 AM');
    expect(fixture.nativeElement.textContent).not.toContain('10:00 AM');
  });
});
