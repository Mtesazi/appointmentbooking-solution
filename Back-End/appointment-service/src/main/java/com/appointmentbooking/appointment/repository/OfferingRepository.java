package com.appointmentbooking.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentbooking.appointment.entity.Offering;

public interface OfferingRepository extends JpaRepository<Offering, Long> {
	List<Offering> findAllByActiveTrueOrderByIdAsc();
}
