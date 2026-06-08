package org.example.meetcare.dao;

import org.example.meetcare.model.Appointment;
import org.example.meetcare.support.H2IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentDaoTest extends H2IntegrationTest {
    @Test
    void findAllAppointmentsAndFindByUserShouldReturnSeededRows() {
        AppointmentDao appointmentDao = new AppointmentDao();
        List<Appointment> all = appointmentDao.findAllAppointments();
        List<Appointment> patientAppointments = appointmentDao.findByUserId(seed.patientUserId());
        List<Appointment> doctorAppointments = appointmentDao.findByDoctorUserId(seed.verifiedDoctorUserId());

        assertEquals(5, all.size());
        assertEquals(5, patientAppointments.size());
        assertEquals(5, doctorAppointments.size());
    }

    @Test
    void updateStatusShouldPersistTheNewValue() {
        AppointmentDao appointmentDao = new AppointmentDao();
        appointmentDao.updateStatus(seed.appointmentPendingId(), "CONFIRME");

        Appointment updated = appointmentDao.findAllAppointments().stream()
                .filter(appointment -> appointment.getId() == seed.appointmentPendingId())
                .findFirst()
                .orElseThrow();

        assertEquals("CONFIRME", updated.getStatus());
    }

    @Test
    void isSlotAvailableShouldDetectConflictsAndFreeSlots() {
        AppointmentDao appointmentDao = new AppointmentDao();
        Appointment seeded = appointmentDao.findAllAppointments().stream()
                .filter(appointment -> appointment.getId() == seed.appointmentTodayId())
                .findFirst()
                .orElseThrow();

        assertFalse(appointmentDao.isSlotAvailable(seed.verifiedDoctorId(), seeded.getDate(), seeded.getTime()));
        assertTrue(appointmentDao.isSlotAvailable(seed.verifiedDoctorId(), LocalDate.now().plusDays(10), "13:15"));
    }
}
