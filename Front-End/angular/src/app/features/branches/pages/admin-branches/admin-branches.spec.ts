import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AdminDataService, BranchDayHours } from '../../../admin/services/admin-data.service';
import { AdminBranches } from './admin-branches';

describe('Branch opening hours', () => {
  const branch = { id: 1, name: 'Central', active: true };
  const initial: BranchDayHours[] = Array.from({ length: 7 }, (_, index) =>
    ({ dayOfWeek: index + 1, closed: false, openTime: '09:00', closeTime: '17:00' }));
  const api = { getBranches: vi.fn(), getBranchHours: vi.fn(), saveBranchHours: vi.fn() };

  beforeEach(async () => {
    api.getBranches.mockReset().mockReturnValue(of([branch]));
    api.getBranchHours.mockReset().mockReturnValue(of(initial.map(day => ({ ...day }))));
    api.saveBranchHours.mockReset();
    await TestBed.configureTestingModule({
      imports: [AdminBranches],
      providers: [{ provide: AdminDataService, useValue: api }]
    }).compileComponents();
  });

  it('saves seven days and clears times on closed days', () => {
    const result = new Subject<BranchDayHours[]>();
    api.saveBranchHours.mockReturnValue(result);
    const fixture = TestBed.createComponent(AdminBranches);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openHours(branch);
    component.hours[0].closed = true;
    component.hours[1].openTime = '11:00';
    component.saveHours();
    expect(api.saveBranchHours).toHaveBeenCalledWith(1, expect.arrayContaining([
      { dayOfWeek: 1, closed: true, openTime: null, closeTime: null },
      { dayOfWeek: 2, closed: false, openTime: '11:00', closeTime: '17:00' }
    ]));
    result.next(initial);
    expect(component.hoursBranch).toBeNull();
    expect(component.announcement).toContain('opening hours updated');
  });

  it('preserves edits and shows schedule conflicts', () => {
    const result = new Subject<BranchDayHours[]>();
    api.saveBranchHours.mockReturnValue(result);
    const fixture = TestBed.createComponent(AdminBranches);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openHours(branch);
    component.hours[0].closed = true;
    component.saveHours();
    result.error(new HttpErrorResponse({ status: 409, error: { message: 'Existing appointments conflict' } }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Existing appointments conflict');
    expect(component.hours[0].closed).toBe(true);
  });
});
