package com.appointmentbooking.appointment.service.impl;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.AuthResponse;
import com.appointmentbooking.appointment.dto.CustomerResponse;
import com.appointmentbooking.appointment.dto.LoginRequest;
import com.appointmentbooking.appointment.dto.RegisterRequest;
import com.appointmentbooking.appointment.entity.Customer;
import com.appointmentbooking.appointment.exception.ConflictException;
import com.appointmentbooking.appointment.exception.ResourceNotFoundException;
import com.appointmentbooking.appointment.exception.UnauthorizedException;
import com.appointmentbooking.appointment.mapper.CustomerMapper;
import com.appointmentbooking.appointment.repository.CustomerRepository;
import com.appointmentbooking.appointment.service.AuthenticationService;
import com.appointmentbooking.appointment.service.TokenService;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
	private final CustomerRepository customerRepository;
	private final PasswordEncoder passwordEncoder;
	private final CustomerMapper customerMapper;
	private final TokenService tokenService;

	public AuthenticationServiceImpl(CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
			CustomerMapper customerMapper, TokenService tokenService) {
		this.customerRepository = customerRepository;
		this.passwordEncoder = passwordEncoder;
		this.customerMapper = customerMapper;
		this.tokenService = tokenService;
	}

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (customerRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("An account with this email already exists");
		}

		Customer customer = customerRepository.save(new Customer(request.firstName().trim(),
				request.lastName().trim(), email, request.phone().trim(),
				passwordEncoder.encode(request.password())));
		return createAuthResponse(customer);
	}

	@Override
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		Customer customer = customerRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
		if (!passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
			throw new UnauthorizedException("Invalid email or password");
		}
		return createAuthResponse(customer);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerResponse getCustomer(Long customerId) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
		return customerMapper.toResponse(customer);
	}

	private AuthResponse createAuthResponse(Customer customer) {
		return new AuthResponse(tokenService.createToken(customer), "Bearer",
				tokenService.getTokenLifetimeSeconds(), customerMapper.toResponse(customer));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
