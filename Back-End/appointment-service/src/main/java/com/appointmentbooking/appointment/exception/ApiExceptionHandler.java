package com.appointmentbooking.appointment.exception;

import java.time.Instant;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.appointmentbooking.appointment.dto.ApiErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception) {
		return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiErrorResponse> conflict(ConflictException exception) {
		return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sql && "23P01".equals(sql.getSQLState())
					&& sql.getMessage() != null && sql.getMessage().contains("appointments_no_confirmed_overlap")) {
				return response(HttpStatus.CONFLICT, "This time is no longer available at the selected branch", Map.of());
			}
			if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())
					&& sql.getMessage() != null && (sql.getMessage().contains("services_name_ignore_case")
							|| sql.getMessage().contains("uk_services_name"))) {
				return response(HttpStatus.CONFLICT, "A service with this name already exists", Map.of());
			}
		}
		return response(HttpStatus.CONFLICT, "The request conflicts with existing data", Map.of());
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiErrorResponse> unauthorized(UnauthorizedException exception) {
		return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> badRequest(BadRequestException exception) {
		return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return response(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
	}

	private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message,
			Map<String, String> validationErrors) {
		return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(), status.value(),
				status.getReasonPhrase(), message, validationErrors));
	}
}
