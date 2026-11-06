package com.appointmentbooking.appointment.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.AdminBranchResponse;
import com.appointmentbooking.appointment.dto.SaveBranchRequest;
import com.appointmentbooking.appointment.entity.Branch;
import com.appointmentbooking.appointment.exception.ConflictException;
import com.appointmentbooking.appointment.exception.ResourceNotFoundException;
import com.appointmentbooking.appointment.repository.BranchRepository;
import com.appointmentbooking.appointment.service.AdminBranchService;

@Service
@Transactional
public class AdminBranchServiceImpl implements AdminBranchService {
	private final BranchRepository branchRepository;

	public AdminBranchServiceImpl(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AdminBranchResponse> getBranches() {
		return branchRepository.findAllByOrderByIdAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Override
	public AdminBranchResponse createBranch(SaveBranchRequest request) {
		String name = request.name().trim();
		if (branchRepository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("A branch with this name already exists");
		}
		return toResponse(branchRepository.save(new Branch(name, request.active())));
	}

	@Override
	public AdminBranchResponse updateBranch(Long id, SaveBranchRequest request) {
		Branch branch = branchRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		String name = request.name().trim();
		if (branchRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("A branch with this name already exists");
		}
		branch.update(name, request.active());
		return toResponse(branchRepository.save(branch));
	}

	@Override
	public void deactivateBranch(Long id) {
		Branch branch = branchRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		branch.deactivate();
		branchRepository.save(branch);
	}

	private AdminBranchResponse toResponse(Branch branch) {
		return new AdminBranchResponse(branch.getId(), branch.getName(), branch.isActive());
	}
}
