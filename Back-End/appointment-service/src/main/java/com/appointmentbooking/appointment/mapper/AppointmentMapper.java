package com.appointmentbooking.appointment.mapper;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.AppointmentSummary;
import com.appointmentbooking.appointment.entity.Appointment;
import com.appointmentbooking.appointment.entity.Branch;
import com.appointmentbooking.appointment.entity.Offering;

@Component
public class AppointmentMapper {
	private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a");

	public AppointmentResponse toResponse(Appointment appointment, Branch branch, Offering offering) {
		return new AppointmentResponse(appointment.getId(), branch.getName(), offering.getName(),
				appointment.getAppointmentDate(), appointment.getStartTime().format(DISPLAY_TIME),
				appointment.getStatus(), appointment.getNotes());
	}

	public AppointmentResponse toResponse(AppointmentSummary summary) {
		return new AppointmentResponse(summary.id(), summary.branch(), summary.service(),
				summary.date(), summary.startTime().format(DISPLAY_TIME), summary.status(), summary.notes());
	}
}
