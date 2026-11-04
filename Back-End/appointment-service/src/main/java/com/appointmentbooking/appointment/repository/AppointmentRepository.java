package com.appointmentbooking.appointment.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.appointmentbooking.appointment.entity.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
	List<Appointment> findAllByCustomerIdOrderByAppointmentDateDescStartTimeDesc(Long customerId);

	Optional<Appointment> findByIdAndCustomerId(Long id, Long customerId);

	@Modifying
	@Query("""
			update Appointment a set a.status = :completedStatus
			where a.customerId = :customerId
			  and a.status = :confirmedStatus
			  and (a.appointmentDate < :date or (a.appointmentDate = :date and a.endTime <= :time))
			""")
	int completeExpiredAppointments(@Param("customerId") Long customerId,
			@Param("date") LocalDate date, @Param("time") LocalTime time,
			@Param("confirmedStatus") Appointment.Status confirmedStatus,
			@Param("completedStatus") Appointment.Status completedStatus);

	@Query("""
			select (count(a) > 0) from Appointment a
			where a.branchId = :branchId
			  and a.appointmentDate = :date
			  and a.status = :status
			  and a.startTime < :endTime
			  and a.endTime > :startTime
			""")
	boolean existsOverlappingAppointment(@Param("branchId") Long branchId,
			@Param("date") LocalDate date,
			@Param("startTime") LocalTime startTime,
			@Param("endTime") LocalTime endTime,
			@Param("status") Appointment.Status status);
}
