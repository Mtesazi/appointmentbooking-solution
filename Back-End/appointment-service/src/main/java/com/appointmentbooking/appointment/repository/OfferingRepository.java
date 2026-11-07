package com.appointmentbooking.appointment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.appointmentbooking.appointment.entity.Offering;

public interface OfferingRepository extends JpaRepository<Offering, Long> {
	List<Offering> findAllByActiveTrueOrderByIdAsc();
	List<Offering> findAllByOrderByIdAsc();
	boolean existsByNameIgnoreCase(String name);
	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("select o from Offering o where o.id = :id")
	Optional<Offering> findByIdForBooking(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Offering o where o.id = :id")
	Optional<Offering> findByIdForUpdate(@Param("id") Long id);

	long countByActiveTrue();
}
