import { Injectable } from '@angular/core';
import { AdminBranch } from '../branch.model';

@Injectable({ providedIn: 'root' })
export class BranchPreviewService {
  private readonly branches: AdminBranch[] = [
    { id: 1, name: 'Cape Town Central', active: true },
    { id: 2, name: 'Bellville Branch', active: true },
    { id: 3, name: 'Stellenbosch Branch', active: true },
    { id: 4, name: 'Durbanville Branch', active: false }
  ];

  getBranches(): AdminBranch[] {
    return this.branches;
  }

  addBranch(name: string, active: boolean): AdminBranch {
    const branch: AdminBranch = {
      id: Math.max(0, ...this.branches.map(item => item.id)) + 1,
      name: name.trim(),
      active
    };
    this.branches.push(branch);
    return branch;
  }

  updateBranch(id: number, name: string, active: boolean): boolean {
    const branch = this.branches.find(item => item.id === id);
    if (!branch) {
      return false;
    }

    branch.name = name.trim();
    branch.active = active;
    return true;
  }

  deleteBranch(id: number): boolean {
    const index = this.branches.findIndex(item => item.id === id);
    if (index < 0) {
      return false;
    }

    this.branches.splice(index, 1);
    return true;
  }
}
