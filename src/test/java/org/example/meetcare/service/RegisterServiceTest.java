package org.example.meetcare.service;

import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.support.H2IntegrationTest;
import org.example.meetcare.utils.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterServiceTest extends H2IntegrationTest {
    @Test
    void registerShouldPersistPatientWithVerifiedStatus() {
        RegisterService registerService = new RegisterService();
        UsersDao usersDao = new UsersDao();
        Users user = buildUser("emma", "Emma", "emma@meetcare.test", "new-pass", Role.USER);

        Users registered = registerService.register(user, "new-pass");
        Users reloaded = usersDao.findById(registered.getIdUsers()).orElseThrow();

        assertTrue(registered.getIdUsers() > 0);
        assertEquals(AccountStatus.VERIFIED, registered.getAccountStatus());
        assertEquals(AccountStatus.VERIFIED, reloaded.getAccountStatus());
        assertFalse(reloaded.isOnline());
        assertNotNull(reloaded.getPassword());
    }

    @Test
    void registerShouldCreateDoctorProfileForMedicalAccounts() {
        RegisterService registerService = new RegisterService();
        DoctorDao doctorDao = new DoctorDao();
        Users user = buildUser("nora", "Nora", "nora@meetcare.test", "doctor-pass", Role.MEDECIN);

        Users registered = registerService.register(user, "doctor-pass");

        assertEquals(AccountStatus.PENDING, registered.getAccountStatus());
        assertTrue(doctorDao.findDoctorIdByUserId(registered.getIdUsers()).isPresent());
    }

    @Test
    void loginShouldAcceptBcryptAndRejectInvalidCredentials() {
        RegisterService registerService = new RegisterService();
        Users loggedIn = registerService.login("patient", "patient-pass");

        assertEquals(seed.patientUserId(), loggedIn.getIdUsers());
        assertTrue(PasswordUtil.isBcryptHash(loggedIn.getPassword()));
        assertThrows(IllegalArgumentException.class, () -> registerService.login("patient", "wrong"));
    }

    private Users buildUser(String username, String prenom, String email, String password, Role role) {
        Users user = new Users();
        user.setUsername(username);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setPassword(password);
        user.setAddress("12 rue des Tests");
        user.setCity("Paris");
        user.setCodePostal("75000");
        user.setCountry("France");
        user.setPhone("0600000099");
        user.setRole(role);
        return user;
    }
}
