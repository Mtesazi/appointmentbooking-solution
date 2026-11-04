import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminBranch } from '../../branch.model';
import { BranchPreviewService } from '../../services/branch-preview.service';

@Component({
  selector: 'app-admin-branches',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-branches.html',
  styleUrl: './admin-branches.scss'
})
export class AdminBranches {
  private readonly preview = inject(BranchPreviewService);

  searchTerm = '';
  selectedStatus: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';
  isFormOpen = false;
  editingId: number | null = null;
  branchName = '';
  branchActive = true;
  formError = '';
  announcement = '';

  get branches(): AdminBranch[] {
    return this.preview.getBranches();
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

    if (this.editingId === null) {
      this.preview.addBranch(name, this.branchActive);
      this.announcement = `${name} added to the preview.`;
    } else {
      const updated = this.preview.updateBranch(this.editingId, name, this.branchActive);
      if (!updated) {
        this.formError = 'This branch is no longer available. Refresh the list and try again.';
        return;
      }
      this.announcement = `${name} updated in the preview.`;
    }

    this.closeForm();
  }

  deleteBranch(branch: AdminBranch): void {
    if (!window.confirm(`Remove ${branch.name} from this preview?`)) {
      return;
    }

    if (!this.preview.deleteBranch(branch.id)) {
      this.announcement = 'This branch is no longer available in the preview.';
      return;
    }

    this.announcement = `${branch.name} removed from the preview.`;
  }

  trackBranch(_index: number, branch: AdminBranch): number {
    return branch.id;
  }
}
