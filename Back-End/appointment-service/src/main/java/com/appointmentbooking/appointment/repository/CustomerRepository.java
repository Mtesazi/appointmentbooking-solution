package com.appointmentbooking.appointment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentbooking.appointment.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	long countByRole(Customer.Role role);

	default long countCustomers() {
		return countByRole(Customer.Role.CUSTOMER);
	}
}
