package com.appointmentbooking.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentbooking.appointment.entity.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {
	List<Branch> findAllByActiveTrueOrderByIdAsc();
}
