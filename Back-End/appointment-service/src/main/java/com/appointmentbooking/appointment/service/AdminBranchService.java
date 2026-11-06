package com.appointmentbooking.appointment.service;

import java.util.List;

import com.appointmentbooking.appointment.dto.AdminBranchResponse;
import com.appointmentbooking.appointment.dto.SaveBranchRequest;

public interface AdminBranchService {
	List<AdminBranchResponse> getBranches();

	AdminBranchResponse createBranch(SaveBranchRequest request);

	AdminBranchResponse updateBranch(Long id, SaveBranchRequest request);

	void deactivateBranch(Long id);
}
