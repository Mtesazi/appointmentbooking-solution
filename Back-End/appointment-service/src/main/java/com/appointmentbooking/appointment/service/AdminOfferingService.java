package com.appointmentbooking.appointment.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.appointmentbooking.appointment.dto.AdminServiceResponse;
import com.appointmentbooking.appointment.dto.SaveServiceRequest;
import com.appointmentbooking.appointment.entity.Offering;
import com.appointmentbooking.appointment.exception.ConflictException;
import com.appointmentbooking.appointment.exception.ResourceNotFoundException;
import com.appointmentbooking.appointment.repository.OfferingRepository;

@Service
public class AdminOfferingService {
	private final OfferingRepository offerings;

	public AdminOfferingService(OfferingRepository offerings) {
		this.offerings = offerings;
	}

	@Transactional(readOnly = true)
	public List<AdminServiceResponse> getServices() {
		return offerings.findAllByOrderByIdAsc().stream().map(this::response).toList();
	}

	@Transactional
	public AdminServiceResponse create(SaveServiceRequest request) {
		if (offerings.existsByNameIgnoreCase(request.name())) {
			throw new ConflictException("A service with this name already exists");
		}
		return response(offerings.save(new Offering(request.name(), request.durationMinutes(), request.active())));
	}

	@Transactional
	public AdminServiceResponse update(Long id, SaveServiceRequest request) {
		Offering offering = offerings.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		if (offerings.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
			throw new ConflictException("A service with this name already exists");
		}
		offering.update(request.name(), request.durationMinutes(), request.active());
		return response(offering);
	}

	private AdminServiceResponse response(Offering offering) {
		return new AdminServiceResponse(offering.getId(), offering.getName(), offering.getDurationMinutes(), offering.isActive());
	}
}
