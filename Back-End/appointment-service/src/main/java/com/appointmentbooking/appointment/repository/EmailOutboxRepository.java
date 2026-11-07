package com.appointmentbooking.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.appointmentbooking.appointment.entity.EmailOutboxMessage;

public interface EmailOutboxRepository extends JpaRepository<EmailOutboxMessage, Long> {
	@Query(value = """
			SELECT * FROM email_outbox
			WHERE status IN ('PENDING', 'FAILED') AND next_attempt_at <= CURRENT_TIMESTAMP
			ORDER BY created_at ASC
			LIMIT 20
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<EmailOutboxMessage> findNextBatchForDelivery();
}
