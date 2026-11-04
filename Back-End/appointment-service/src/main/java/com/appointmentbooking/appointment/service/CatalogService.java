package com.appointmentbooking.appointment.service;

import java.util.List;

import com.appointmentbooking.appointment.dto.BranchResponse;
import com.appointmentbooking.appointment.dto.ServiceResponse;

public interface CatalogService {
	List<BranchResponse> getBranches();

	List<ServiceResponse> getServices();
}
