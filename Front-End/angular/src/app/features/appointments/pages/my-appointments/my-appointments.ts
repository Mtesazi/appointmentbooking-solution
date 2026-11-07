import { ChangeDetectorRef, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { AppointmentService } from '../../appointment.service';
import { Appointment } from '../../appointment.model';
import { getApiErrorMessage } from '../../../../core/api-error';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.scss'
})
export class MyAppointments implements OnInit {
  private readonly appointmentService = inject(AppointmentService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly availabilityRequests = new Subject<{ id: number; date: string } | null>();

  appointments: Appointment[] = [];
  loading = true;
  errorMessage = '';
  cancellingId: number | null = null;
  rescheduling: Appointment | null = null;
  rescheduleDate = '';
  rescheduleTime = '';
  availableTimes: string[] = [];
  loadingTimes = false;
  timesLoaded = false;
  rescheduleSaving = false;
  rescheduleError = '';
  successMessage = '';
  readonly today = this.localDate();

  ngOnInit(): void {
    this.availabilityRequests.pipe(
      switchMap(request => {
        if (!request) {
          return EMPTY;
        }
        return this.appointmentService.getRescheduleAvailableTimes(request.id, request.date).pipe(
          catchError(error => {
            this.loadingTimes = false;
            this.rescheduleError = getApiErrorMessage(error);
            this.changeDetector.markForCheck();
            return EMPTY;
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(times => {
      this.availableTimes = times;
      this.loadingTimes = false;
      this.timesLoaded = true;
      this.changeDetector.markForCheck();
    });
    this.loadAppointments();
  }

  startReschedule(appointment: Appointment): void {
    if (this.rescheduleSaving || this.cancellingId !== null) {
      return;
    }
    this.rescheduling = appointment;
    this.rescheduleDate = appointment.date;
    this.successMessage = '';
    this.loadAvailableTimes();
  }

  changeDate(value: string): void {
    this.rescheduleDate = value;
    this.loadAvailableTimes();
  }

  loadAvailableTimes(): void {
    this.rescheduleTime = '';
    this.availableTimes = [];
    this.timesLoaded = false;
    this.rescheduleError = '';
    const appointment = this.rescheduling;
    this.loadingTimes = !!appointment && !!this.rescheduleDate;
    this.availabilityRequests.next(this.loadingTimes && appointment
      ? { id: appointment.id, date: this.rescheduleDate } : null);
  }

  closeReschedule(): void {
    if (this.rescheduleSaving) {
      return;
    }
    this.availabilityRequests.next(null);
    this.rescheduling = null;
    this.loadingTimes = false;
  }

  submitReschedule(): void {
    if (!this.rescheduling || !this.rescheduleDate || !this.rescheduleTime
        || this.loadingTimes || this.rescheduleSaving) {
      return;
    }
    this.rescheduleSaving = true;
    this.rescheduleError = '';
    this.appointmentService.rescheduleAppointment(
      this.rescheduling.id, this.rescheduleDate, this.rescheduleTime
    ).subscribe({
      next: updated => {
        this.appointments = this.appointments.map(item => item.id === updated.id ? updated : item);
        this.rescheduleSaving = false;
        this.closeReschedule();
        this.successMessage = 'Your appointment has been rescheduled.';
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.rescheduleSaving = false;
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.loadAvailableTimes();
        }
        this.rescheduleError = getApiErrorMessage(error);
        this.changeDetector.markForCheck();
      }
    });
  }

  private localDate(): string {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';
    const navigationState = (history.state ?? {}) as { appointment?: Appointment; appointments?: Appointment[] };
    const cachedAppointments = this.appointmentService.getCachedAppointments() ?? [];
    const navigationAppointments = [
      ...(Array.isArray(navigationState.appointments) ? navigationState.appointments : []),
      ...(navigationState.appointment ? [navigationState.appointment] : [])
    ];
    this.appointments = [...navigationAppointments, ...cachedAppointments]
      .filter((appointment, index, list) =>
        list.findIndex(item => item.id === appointment.id) === index
      );

    this.appointmentService.getAppointments().subscribe({
      next: appointments => {
        if (appointments.length > 0) {
          this.appointments = appointments;
          this.loading = false;
          this.changeDetector.markForCheck();
          return;
        }

        if (this.appointments.length > 0) {
          this.loading = false;
          this.changeDetector.markForCheck();
          return;
        }

        this.appointmentService.getDashboard().subscribe({
          next: dashboard => {
            this.appointments = dashboard.recentAppointments;
            this.loading = false;
            this.changeDetector.markForCheck();
          },
          error: error => {
            this.errorMessage = getApiErrorMessage(error);
            this.loading = false;
            this.changeDetector.markForCheck();
          }
        });
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  cancelAppointment(appointment: Appointment): void {
    if (appointment.status !== 'CONFIRMED' || this.cancellingId !== null) {
      return;
    }

    this.cancellingId = appointment.id;
    this.errorMessage = '';
    this.successMessage = '';
    this.appointmentService.cancelAppointment(appointment.id).subscribe({
      next: updatedAppointment => {
        this.appointments = this.appointments.map(item =>
          item.id === updatedAppointment.id ? updatedAppointment : item
        );
        if (this.rescheduling?.id === updatedAppointment.id) {
          this.closeReschedule();
        }
        this.cancellingId = null;
        this.successMessage = 'Your appointment has been cancelled.';
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.cancellingId = null;
        this.changeDetector.markForCheck();
      }
    });
  }

  get upcomingAppointments(): Appointment[] {
    return this.appointments.filter(
      appointment => appointment.status === 'CONFIRMED'
    );
  }

  get pastAppointments(): Appointment[] {
    return this.appointments.filter(
      appointment => appointment.status !== 'CONFIRMED'
    );
  }
}
