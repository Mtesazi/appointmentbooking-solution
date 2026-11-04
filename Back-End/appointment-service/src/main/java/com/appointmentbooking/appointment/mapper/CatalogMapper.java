package com.appointmentbooking.appointment.mapper;

import org.springframework.stereotype.Component;

import com.appointmentbooking.appointment.dto.BranchResponse;
import com.appointmentbooking.appointment.dto.ServiceResponse;
import com.appointmentbooking.appointment.entity.Branch;
import com.appointmentbooking.appointment.entity.Offering;

@Component
public class CatalogMapper {
	public BranchResponse toResponse(Branch branch) {
		return new BranchResponse(branch.getId(), branch.getName());
	}

	public ServiceResponse toResponse(Offering offering) {
		return new ServiceResponse(offering.getId(), offering.getName(), offering.getDurationMinutes());
	}
}
