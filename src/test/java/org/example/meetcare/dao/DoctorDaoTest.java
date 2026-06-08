package org.example.meetcare.dao;

import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Doctor;
import org.example.meetcare.support.H2IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorDaoTest extends H2IntegrationTest {
    @Test
    void updateProfileAndLookupShouldWork() {
        DoctorDao doctorDao = new DoctorDao();

        doctorDao.updateProfile(seed.verifiedDoctorUserId(), "Cardiologie", "12 rue Victor-Hugo", "Secteur 2",
                "70.0", "15", "Spécialiste du coeur.", true, true);

        Doctor doctor = doctorDao.findByUserId(seed.verifiedDoctorUserId()).orElseThrow();

        assertEquals("Cardiologie", doctor.getSpeciality());
        assertEquals("12 rue Victor-Hugo", doctor.getAddress());
        assertTrue(doctor.isTeleconsultation());
    }

    @Test
    void doctorIdentifierLookupShouldCreateMissingProfileWhenNeeded() {
        DoctorDao doctorDao = new DoctorDao();
        int doctorId = doctorDao.ensureDoctorIdForUser(seed.autoDoctorUserId());

        assertTrue(doctorId > 0);
        assertTrue(doctorDao.findDoctorIdByUserId(seed.autoDoctorUserId()).isPresent());
    }

    @Test
    void latestRejectionReasonShouldBeReturned() {
        DoctorDao doctorDao = new DoctorDao();

        assertEquals("Dossier incomplet", doctorDao.findLatestRejectionReasonByUserId(seed.rejectedDoctorUserId()).orElseThrow());
    }
}
