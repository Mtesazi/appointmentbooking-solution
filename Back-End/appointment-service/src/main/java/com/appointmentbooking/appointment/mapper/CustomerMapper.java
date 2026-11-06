package com.appointmentbooking.appointment.mapper;

import org.springframework.stereotype.Component;

import com.appointmentbooking.appointment.dto.CustomerResponse;
import com.appointmentbooking.appointment.entity.Customer;

@Component
public class CustomerMapper {
	public CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(),
				customer.getEmail(), customer.getPhone(), customer.getRole());
	}
}
