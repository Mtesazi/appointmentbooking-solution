import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../authentication/auth.service';
import { Customer } from '../../../authentication/auth.model';
import { Profile } from './profile';

describe('Profile', () => {
  const customer: Customer = {
    id: 1, firstName: 'Alex', lastName: 'Example', phone: '1234567',
    email: 'alex@example.invalid', role: 'CUSTOMER'
  };
  const auth = { getProfile: vi.fn(), updateProfile: vi.fn() };

  beforeEach(async () => {
    auth.getProfile.mockReset().mockReturnValue(of(customer));
    auth.updateProfile.mockReset();
    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }]
    }).compileComponents();
  });

  it('loads the profile, validates and saves the editable fields', () => {
    const saved = new Subject<Customer>();
    auth.updateProfile.mockReturnValue(saved);
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.email).toBe(customer.email);
    component.form.controls.firstName.setValue(' A ');
    component.save();
    expect(auth.updateProfile).not.toHaveBeenCalled();
    component.form.setValue({ firstName: 'New', lastName: 'Name', phone: '123456789' });
    component.save();
    expect(auth.updateProfile).toHaveBeenCalledWith({ firstName: 'New', lastName: 'Name', phone: '123456789' });
    expect(component.saving).toBe(true);
    saved.next({ ...customer, firstName: 'New', lastName: 'Name', phone: '123456789' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Your profile has been updated.');
    expect(component.saving).toBe(false);
  });

  it('preserves edits when saving fails', () => {
    auth.updateProfile.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 500, error: { message: 'Save failed' } })));
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.firstName.setValue('New');
    fixture.componentInstance.save();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Save failed');
    expect(fixture.componentInstance.form.controls.firstName.value).toBe('New');
    expect(fixture.componentInstance.form.enabled).toBe(true);
  });

  it('shows a retry option when profile loading fails', () => {
    auth.getProfile.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 500, error: { message: 'Load failed' } })));
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Load failed');
    expect(fixture.nativeElement.textContent).toContain('Try again');
  });
});
