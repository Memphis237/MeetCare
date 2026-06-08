package org.example.meetcare.controller;

import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.DoctorAdminEntry;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.support.H2IntegrationTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsAdminControllerTest extends H2IntegrationTest {
    @Test
    void statsCountsShouldReflectLongerTermView() throws Exception {
        StatsAdminController controller = new StatsAdminController();
        List<Users> users = buildUsers();
        setField(controller, "loadedUsers", users);
        setField(controller, "loadedDoctors", buildDoctors());
        setField(controller, "loadedAppointments", buildAppointments());

        assertEquals("50", invokeString(controller, "computeVerifiedRate", List.class, users));
        assertEquals(2L, invokeLong(controller, "verifiedDoctorsCount"));
        assertEquals(1L, invokeLong(controller, "pendingDoctorsCount"));
        assertEquals(1L, invokeLong(controller, "rejectedDoctorsCount"));
        assertEquals(3L, invokeLong(controller, "confirmedAppointmentsCount"));
        assertEquals(1L, invokeLong(controller, "pendingAppointmentsCount"));
        assertEquals(1L, invokeLong(controller, "cancelledAppointmentsCount"));
        assertEquals(5L, invokeLong(controller, "totalAppointments"));
        assertEquals("juin 2026", invokeString(controller, "labelMonth", YearMonth.class, YearMonth.of(2026, 6)));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private long invokeLong(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (Long) method.invoke(target);
    }

    private String invokeString(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return (String) method.invoke(target, argument);
    }

    private List<Users> buildUsers() {
        return List.of(
                user(1, "admin", "Alice", Role.ADMIN, AccountStatus.VERIFIED, -12),
                user(2, "patient", "Paul", Role.USER, AccountStatus.VERIFIED, -8),
                user(3, "martin", "Antoine", Role.MEDECIN, AccountStatus.VERIFIED, -10),
                user(4, "dupont", "Claire", Role.MEDECIN, AccountStatus.PENDING, -5),
                user(5, "faure", "Laura", Role.MEDECIN, AccountStatus.REJECTED, -4),
                user(6, "newdoc", "Nadia", Role.MEDECIN, AccountStatus.VERIFIED, -2)
        );
    }

    private List<DoctorAdminEntry> buildDoctors() {
        return List.of(
                doctor(1, 1, "Antoine", "martin", "Cardiologie", "Paris"),
                doctor(2, 2, "Claire", "dupont", "Dermatologie", "Lyon"),
                doctor(3, 3, "Laura", "faure", "Radiologie", "Nice"),
                doctor(4, 4, "Nadia", "newdoc", "Pédiatrie", "Toulouse")
        );
    }

    private List<Appointment> buildAppointments() {
        LocalDate today = LocalDate.now();
        return List.of(
                appointment(1, today, "09:00", "CONFIRME", "NORMAL", true, "Contrôle annuel"),
                appointment(2, today.minusDays(1), "10:30", "EN_ATTENTE", "NORMAL", true, "Premier rendez-vous"),
                appointment(3, today.minusDays(2), "11:15", "ANNULE", "URGENT", true, "Annulé"),
                appointment(4, today.minusDays(3), "16:45", "CONFIRME", "CRITIQUE", true, "Douleur aiguë"),
                appointment(5, today.minusDays(8), "15:00", "CONFIRME", "NORMAL", true, "Hors fenêtre")
        );
    }

    private Users user(int id, String username, String prenom, Role role, AccountStatus status, int daysAgo) {
        Users user = new Users();
        user.setIdUsers(id);
        user.setUsername(username);
        user.setPrenom(prenom);
        user.setRole(role);
        user.setAccountStatus(status);
        user.setEmail(username + "@meetcare.test");
        user.setPhone("0600000000");
        user.setAvatar("avatar.png");
        user.setAddress("Adresse");
        user.setCity("Ville");
        user.setCodePostal("00000");
        user.setCountry("France");
        user.setDate(LocalDateTime.now().minusDays(Math.abs(daysAgo)));
        return user;
    }

    private DoctorAdminEntry doctor(int userId, int doctorId, String prenom, String username, String speciality, String city) {
        return new DoctorAdminEntry(
                userId,
                doctorId,
                prenom,
                username,
                username + "@meetcare.test",
                "0600000000",
                city,
                "Adresse",
                speciality,
                "Adresse",
                10,
                50.0,
                "Bio",
                "Secteur 1",
                true,
                true,
                true,
                AccountStatus.VERIFIED
        );
    }

    private Appointment appointment(int id, LocalDate date, String time, String status, String urgency, boolean teleconsultation, String notes) {
        return new Appointment(
                id,
                "Dr. Martin",
                "Cardiologie",
                "Paris",
                "12 rue Victor-Hugo",
                status,
                urgency,
                notes,
                teleconsultation,
                date,
                time,
                "Paul patient"
        );
    }
}
