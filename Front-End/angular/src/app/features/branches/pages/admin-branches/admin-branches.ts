import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminBranch } from '../../branch.model';
import { AdminDataService } from '../../../admin/services/admin-data.service';
import { getApiErrorMessage } from '../../../../core/api-error';

@Component({
  selector: 'app-admin-branches',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-branches.html',
  styleUrl: './admin-branches.scss'
})
export class AdminBranches implements OnInit {
  private readonly adminData = inject(AdminDataService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  branchList: AdminBranch[] = [];
  loading = true;
  errorMessage = '';
  saving = false;
  searchTerm = '';
  selectedStatus: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';
  isFormOpen = false;
  editingId: number | null = null;
  branchName = '';
  branchActive = true;
  formError = '';
  announcement = '';

  ngOnInit(): void {
    this.loadBranches();
  }

  loadBranches(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminData.getBranches().subscribe({
      next: branches => {
        this.branchList = branches;
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

  get branches(): AdminBranch[] {
    return this.branchList;
  }

  get filteredBranches(): AdminBranch[] {
    const query = this.searchTerm.trim().toLowerCase();

    return this.branches.filter(branch => {
      const matchesStatus =
        this.selectedStatus === 'ALL' ||
        (this.selectedStatus === 'ACTIVE' && branch.active) ||
        (this.selectedStatus === 'INACTIVE' && !branch.active);
      return matchesStatus && branch.name.toLowerCase().includes(query);
    });
  }

  openCreateForm(): void {
    this.editingId = null;
    this.branchName = '';
    this.branchActive = true;
    this.formError = '';
    this.isFormOpen = true;
  }

  openEditForm(branch: AdminBranch): void {
    this.editingId = branch.id;
    this.branchName = branch.name;
    this.branchActive = branch.active;
    this.formError = '';
    this.isFormOpen = true;
  }

  closeForm(): void {
    this.isFormOpen = false;
    this.formError = '';
  }

  saveBranch(): void {
    const name = this.branchName.trim();
    if (!name) {
      this.formError = 'Enter a branch name.';
      return;
    }

    const duplicate = this.branches.some(
      branch =>
        branch.id !== this.editingId &&
        branch.name.localeCompare(name, undefined, { sensitivity: 'accent' }) === 0
    );
    if (duplicate) {
      this.formError = 'A branch with this name already exists.';
      return;
    }

    this.saving = true;
    const request = { name, active: this.branchActive };
    const saveRequest = this.editingId === null
      ? this.adminData.createBranch(request)
      : this.adminData.updateBranch(this.editingId, request);
    saveRequest.subscribe({
      next: savedBranch => {
        if (this.editingId === null) {
          this.branchList = [...this.branchList, savedBranch];
          this.announcement = `${name} added.`;
        } else {
          this.branchList = this.branchList.map(branch =>
            branch.id === savedBranch.id ? savedBranch : branch
          );
          this.announcement = `${name} updated.`;
        }
        this.saving = false;
        this.closeForm();
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.formError = getApiErrorMessage(error);
        this.saving = false;
        this.changeDetector.markForCheck();
      }
    });
  }

  deleteBranch(branch: AdminBranch): void {
    if (!window.confirm(`Deactivate ${branch.name}? Existing appointments will be retained.`)) {
      return;
    }

    this.adminData.deactivateBranch(branch.id).subscribe({
      next: () => {
        this.branchList = this.branchList.map(item =>
          item.id === branch.id ? { ...item, active: false } : item
        );
        this.announcement = `${branch.name} deactivated.`;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.errorMessage = getApiErrorMessage(error);
        this.changeDetector.markForCheck();
      }
    });
  }

  trackBranch(_index: number, branch: AdminBranch): number {
    return branch.id;
  }
}
