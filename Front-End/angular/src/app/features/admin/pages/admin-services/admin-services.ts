import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { getApiErrorMessage } from '../../../../core/api-error';
import { AdminDataService, AdminServiceOption } from '../../services/admin-data.service';

@Component({
  selector: 'app-admin-services',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-services.html',
  styleUrl: './admin-services.scss'
})
export class AdminServices implements OnInit {
  private readonly api = inject(AdminDataService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  services: AdminServiceOption[] = [];
  loading = true;
  saving = false;
  errorMessage = '';
  formError = '';
  announcement = '';
  editing = false;
  editingId: number | null = null;
  name = '';
  durationMinutes: number | null = 30;
  active = true;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.api.getServices().subscribe({
      next: services => {
        this.services = services;
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

  edit(service?: AdminServiceOption): void {
    this.editingId = service?.id ?? null;
    this.name = service?.name ?? '';
    this.durationMinutes = service?.durationMinutes ?? 30;
    this.active = service?.active ?? true;
    this.formError = '';
    this.announcement = '';
    this.editing = true;
  }

  close(): void {
    if (!this.saving) {
      this.editing = false;
    }
  }

  save(): void {
    const name = this.name.trim();
    const duration = this.durationMinutes;
    if (!name || name.length > 120 || duration === null || !Number.isInteger(duration) || duration < 1 || duration > 1440) {
      this.formError = 'Enter a service name and a whole-number duration from 1 to 1440 minutes.';
      return;
    }
    const request = { name, durationMinutes: duration, active: this.active };
    const id = this.editingId;
    this.saving = true;
    this.formError = '';
    const action = id === null ? this.api.createService(request) : this.api.updateService(id, request);
    action.subscribe({
      next: service => {
        this.services = id === null ? [...this.services, service]
          : this.services.map(item => item.id === service.id ? service : item);
        this.saving = false;
        this.editing = false;
        this.announcement = `${service.name} ${id === null ? 'added' : 'updated'}.`;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.saving = false;
        this.formError = getApiErrorMessage(error);
        this.changeDetector.markForCheck();
      }
    });
  }
}
