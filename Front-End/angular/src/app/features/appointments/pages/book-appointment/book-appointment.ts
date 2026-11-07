import { ChangeDetectorRef, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, merge, Subject, catchError, startWith, switchMap, tap } from 'rxjs';
import { AppointmentService } from '../../appointment.service';
import { Branch } from '../../../branches/branch.model';
import { AppointmentServiceOption } from '../../appointment.model';
import { getApiErrorMessage } from '../../../../core/api-error';

@Component({
  selector: 'app-book-appointment',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './book-appointment.html',
  styleUrl: './book-appointment.scss'
})
export class BookAppointment implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly appointmentService = inject(AppointmentService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshAvailability = new Subject<void>();

  submitted = false;
  loadingCatalog = true;
  submitting = false;
  loadingAvailability = false;
  availabilityLoaded = false;
  availabilityError = '';
  errorMessage = '';
  selectedTime = '';

  today = this.getLocalDate();

  timeSlots: string[] = [];

  branches: Branch[] = [];
  services: AppointmentServiceOption[] = [];

  bookingForm = this.fb.group({
    branchId: ['', Validators.required],
    serviceId: ['', Validators.required],
    appointmentDate: ['', Validators.required]
  });

  ngOnInit(): void {
    merge(this.bookingForm.valueChanges, this.refreshAvailability).pipe(
      startWith(null),
      tap(() => {
        this.selectedTime = '';
        this.timeSlots = [];
        this.availabilityLoaded = false;
        this.availabilityError = '';
      }),
      switchMap(() => {
        const { branchId, serviceId, appointmentDate } = this.bookingForm.getRawValue();
        if (!branchId || !serviceId || !appointmentDate) {
          this.loadingAvailability = false;
          this.changeDetector.markForCheck();
          return EMPTY;
        }
        this.loadingAvailability = true;
        this.changeDetector.markForCheck();
        return this.appointmentService.getAvailableTimes(Number(branchId), Number(serviceId), appointmentDate).pipe(
          catchError(error => {
            this.availabilityError = getApiErrorMessage(error);
            this.loadingAvailability = false;
            this.changeDetector.markForCheck();
            return EMPTY;
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(times => {
      this.timeSlots = times;
      this.loadingAvailability = false;
      this.availabilityLoaded = true;
      this.changeDetector.markForCheck();
    });

    this.appointmentService.getBranches().subscribe({
      next: branches => {
        this.branches = branches;
        this.loadServices();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingCatalog = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  private loadServices(): void {
    this.appointmentService.getServices().subscribe({
      next: services => {
        this.services = services;
        this.loadingCatalog = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingCatalog = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  selectTime(time: string): void {
    this.selectedTime = time;
    this.submitted = false;
    this.errorMessage = '';
  }

  retryAvailability(): void {
    this.refreshAvailability.next();
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.bookingForm.invalid || !this.selectedTime || this.submitting || this.loadingAvailability) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    const formValue = this.bookingForm.getRawValue();

    if (!formValue.branchId || !formValue.serviceId || !formValue.appointmentDate) {
      return;
    }

    this.submitting = true;
    this.bookingForm.disable({ emitEvent: false });
    this.errorMessage = '';
    this.appointmentService.createAppointment({
      branchId: Number(formValue.branchId),
      serviceId: Number(formValue.serviceId),
      appointmentDate: formValue.appointmentDate,
      time: this.selectedTime
    }).subscribe({
      next: appointment => {
        this.appointmentService.saveBookingConfirmation(appointment);
        void this.router.navigate(['/customer/booking-confirmation']);
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.submitting = false;
        this.bookingForm.enable({ emitEvent: false });
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.refreshAvailability.next();
        }
        this.changeDetector.markForCheck();
      }
    });
  }

  get selectedBranchName(): string {
    return this.branches.find(branch => branch.id.toString() === this.bookingForm.controls.branchId.value)?.name
      ?? 'Not selected';
  }

  get selectedServiceName(): string {
    return this.services.find(service => service.id.toString() === this.bookingForm.controls.serviceId.value)?.name
      ?? 'Not selected';
  }

  getLocalDate(): string {
    const date = new Date();

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
