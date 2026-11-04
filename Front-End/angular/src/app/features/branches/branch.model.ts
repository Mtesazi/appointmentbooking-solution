export interface Branch {
  id: number;
  name: string;
}

export interface AdminBranch extends Branch {
  active: boolean;
}
