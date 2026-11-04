package com.appointmentbooking.appointment.dto;

public record CustomerResponse(Long id, String firstName, String lastName, String email, String phone) {
}
