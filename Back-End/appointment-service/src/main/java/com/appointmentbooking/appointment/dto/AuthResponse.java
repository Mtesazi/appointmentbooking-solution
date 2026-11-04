package com.appointmentbooking.appointment.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, CustomerResponse customer) {
}
