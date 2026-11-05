import { Component, inject, OnInit } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
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

  submitted = false;
  loadingCatalog = true;
  submitting = false;
  errorMessage = '';
  selectedTime = '';

  today = this.getLocalDate();

  timeSlots: string[] = [
    '09:00 AM',
    '09:30 AM',
    '10:00 AM',
    '10:30 AM',
    '11:00 AM',
    '11:30 AM',
    '12:00 PM',
    '01:00 PM',
    '01:30 PM',
    '02:00 PM',
    '02:30 PM',
    '03:00 PM',
    '03:30 PM',
    '04:00 PM'
  ];

  branches: Branch[] = [];
  services: AppointmentServiceOption[] = [];

  bookingForm = this.fb.group({
    branchId: ['', Validators.required],
    serviceId: ['', Validators.required],
    appointmentDate: ['', Validators.required]
  });

  ngOnInit(): void {
    this.appointmentService.getBranches().subscribe({
      next: branches => {
        this.branches = branches;
        this.loadServices();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingCatalog = false;
      }
    });
  }

  private loadServices(): void {
    this.appointmentService.getServices().subscribe({
      next: services => {
        this.services = services;
        this.loadingCatalog = false;
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingCatalog = false;
      }
    });
  }

  selectTime(time: string): void {
    this.selectedTime = time;
    this.submitted = false;
    this.errorMessage = '';
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.bookingForm.invalid || !this.selectedTime || this.submitting) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    const formValue = this.bookingForm.getRawValue();

    if (!formValue.branchId || !formValue.serviceId || !formValue.appointmentDate) {
      return;
    }

    this.submitting = true;
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
