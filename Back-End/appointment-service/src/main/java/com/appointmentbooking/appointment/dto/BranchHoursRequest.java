package com.appointmentbooking.appointment.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BranchHoursRequest(@NotNull @Size(min = 7, max = 7) List<@NotNull @Valid Day> days) {
	public record Day(@NotNull Integer dayOfWeek, boolean closed, String openTime, String closeTime) {
	}
}
