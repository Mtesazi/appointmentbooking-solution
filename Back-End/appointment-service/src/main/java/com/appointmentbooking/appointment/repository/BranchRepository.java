package com.appointmentbooking.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.appointmentbooking.appointment.entity.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Branch b where b.id = :id")
	Optional<Branch> findByIdForUpdate(@Param("id") Long id);
	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("select b from Branch b where b.id = :id")
	Optional<Branch> findByIdForBooking(@Param("id") Long id);
	List<Branch> findAllByActiveTrueOrderByIdAsc();

	List<Branch> findAllByOrderByIdAsc();

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
