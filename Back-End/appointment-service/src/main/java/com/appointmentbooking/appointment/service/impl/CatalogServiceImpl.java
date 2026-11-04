package com.appointmentbooking.appointment.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.BranchResponse;
import com.appointmentbooking.appointment.dto.ServiceResponse;
import com.appointmentbooking.appointment.mapper.CatalogMapper;
import com.appointmentbooking.appointment.repository.BranchRepository;
import com.appointmentbooking.appointment.repository.OfferingRepository;
import com.appointmentbooking.appointment.service.CatalogService;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {
	private final BranchRepository branchRepository;
	private final OfferingRepository offeringRepository;
	private final CatalogMapper catalogMapper;

	public CatalogServiceImpl(BranchRepository branchRepository, OfferingRepository offeringRepository,
			CatalogMapper catalogMapper) {
		this.branchRepository = branchRepository;
		this.offeringRepository = offeringRepository;
		this.catalogMapper = catalogMapper;
	}

	@Override
	public List<BranchResponse> getBranches() {
		return branchRepository.findAllByActiveTrueOrderByIdAsc().stream()
				.map(catalogMapper::toResponse).toList();
	}

	@Override
	public List<ServiceResponse> getServices() {
		return offeringRepository.findAllByActiveTrueOrderByIdAsc().stream()
				.map(catalogMapper::toResponse).toList();
	}
}
