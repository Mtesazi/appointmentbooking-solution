import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { AppointmentService } from '../../appointment.service';

@Component({
  selector: 'app-book-appointment',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './book-appointment.html',
  styleUrl: './book-appointment.scss'
})
export class BookAppointment {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly appointmentService = inject(AppointmentService);

  submitted = false;
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

  branches = [
    { id: 1, name: 'Cape Town Central' },
    { id: 2, name: 'Bellville Branch' },
    { id: 3, name: 'Stellenbosch Branch' },
    { id: 4, name: 'Durbanville Branch' }
  ];

  services = [
    { id: 1, name: 'General Consultation', duration: '30 minutes' },
    { id: 2, name: 'Follow-up Consultation', duration: '20 minutes' },
    { id: 3, name: 'Professional Consultation', duration: '45 minutes' },
    { id: 4, name: 'Specialist Consultation', duration: '60 minutes' }
  ];

  bookingForm = this.fb.group({
    branchId: ['', Validators.required],
    serviceId: ['', Validators.required],
    appointmentDate: ['', Validators.required]
  });

  selectTime(time: string): void {
    this.selectedTime = time;
    this.submitted = false;
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.bookingForm.invalid || !this.selectedTime) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    const formValue = this.bookingForm.getRawValue();

    const selectedBranch = this.branches.find(
      branch => branch.id.toString() === formValue.branchId
    );

    const selectedService = this.services.find(
      service => service.id.toString() === formValue.serviceId
    );

    if (!selectedBranch || !selectedService || !formValue.appointmentDate) {
      return;
    }

    const appointment = {
      id: crypto.randomUUID(),
      branch: selectedBranch.name,
      service: selectedService.name,
      date: formValue.appointmentDate,
      time: this.selectedTime,
      status: 'CONFIRMED' as const
    };

    this.appointmentService.saveAppointment(appointment);

    this.router.navigate(['/customer/booking-confirmation']);
  }

  getLocalDate(): string {
    const date = new Date();

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
