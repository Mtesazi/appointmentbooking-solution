import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AdminDataService, AdminServiceOption } from '../../services/admin-data.service';
import { AdminServices } from './admin-services';

describe('AdminServices', () => {
  const service: AdminServiceOption = { id: 1, name: 'Consultation', durationMinutes: 30, active: true };
  const api = { getServices: vi.fn(), createService: vi.fn(), updateService: vi.fn() };

  beforeEach(async () => {
    api.getServices.mockReset().mockReturnValue(of([service]));
    api.createService.mockReset();
    api.updateService.mockReset();
    await TestBed.configureTestingModule({
      imports: [AdminServices],
      providers: [{ provide: AdminDataService, useValue: api }]
    }).compileComponents();
  });

  it('creates a validated service and displays it', () => {
    const saved = new Subject<AdminServiceOption>();
    api.createService.mockReturnValue(saved);
    const fixture = TestBed.createComponent(AdminServices);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.edit();
    component.name = '  New service  ';
    component.durationMinutes = 0;
    component.save();
    expect(api.createService).not.toHaveBeenCalled();
    component.durationMinutes = 45;
    component.save();
    expect(api.createService).toHaveBeenCalledWith({ name: 'New service', durationMinutes: 45, active: true });
    saved.next({ id: 2, name: 'New service', durationMinutes: 45, active: true });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('New service added.');
  });

  it('supports deactivation and preserves edits when saving fails', () => {
    const saved = new Subject<AdminServiceOption>();
    api.updateService.mockReturnValue(saved);
    const fixture = TestBed.createComponent(AdminServices);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.edit(service);
    component.active = false;
    component.save();
    expect(api.updateService).toHaveBeenCalledWith(1, { name: 'Consultation', durationMinutes: 30, active: false });
    saved.error(new HttpErrorResponse({ status: 409, error: { message: 'Duplicate name' } }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Duplicate name');
    expect(component.editing).toBe(true);
    expect(component.active).toBe(false);
  });
});
