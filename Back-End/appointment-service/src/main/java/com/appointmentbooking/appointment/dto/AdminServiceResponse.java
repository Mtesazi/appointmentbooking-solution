package com.appointmentbooking.appointment.dto;

public record AdminServiceResponse(Long id, String name, int durationMinutes, boolean active) {
}
