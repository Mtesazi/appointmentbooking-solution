import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../../../core/api-error';
import { AuthService } from '../../../authentication/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/^\s*\S(?:.*\S)\s*$/)]],
    lastName: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/^\s*\S(?:.*\S)\s*$/)]],
    phone: ['', [Validators.required, Validators.maxLength(30),
      Validators.pattern(/^\s*[0-9+()\-][0-9+()\-\s]{5,28}[0-9+()\-]\s*$/)]]
  });
  email = '';
  loading = true;
  saving = false;
  loaded = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.loaded = false;
    this.errorMessage = '';
    this.auth.getProfile().subscribe({
      next: customer => {
        this.form.reset({ firstName: customer.firstName, lastName: customer.lastName, phone: customer.phone });
        this.email = customer.email;
        this.loaded = true;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  save(): void {
    if (!this.loaded || this.saving) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.form.disable();
    this.auth.updateProfile(this.form.getRawValue()).subscribe({
      next: customer => {
        this.form.enable();
        this.form.reset({ firstName: customer.firstName, lastName: customer.lastName, phone: customer.phone });
        this.email = customer.email;
        this.saving = false;
        this.successMessage = 'Your profile has been updated.';
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.form.enable();
        this.saving = false;
        this.errorMessage = getApiErrorMessage(error);
        this.changeDetector.markForCheck();
      }
    });
  }
}
