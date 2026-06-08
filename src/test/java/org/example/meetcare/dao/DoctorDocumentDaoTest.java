package org.example.meetcare.dao;

import org.example.meetcare.model.DoctorDocumentEntry;
import org.example.meetcare.support.H2IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoctorDocumentDaoTest extends H2IntegrationTest {
    @Test
    void pendingDocumentsShouldBeGroupedByDoctor() {
        DoctorDocumentDao doctorDocumentDao = new DoctorDocumentDao();
        List<DoctorDocumentEntry> pending = doctorDocumentDao.findPendingDocumentsForAdmin();

        assertEquals(2, pending.size());
        assertEquals(1, doctorDocumentDao.countPendingDoctorsForAdmin());
    }

    @Test
    void rejectedDocumentCounterShouldMatchSeededData() {
        DoctorDocumentDao doctorDocumentDao = new DoctorDocumentDao();
        assertEquals(1, doctorDocumentDao.countRejectedDocumentsForDoctorUserId(seed.rejectedDoctorUserId()));
    }

    @Test
    void setVerificationStatusForDocumentsShouldUpdateSelectedRows() {
        DoctorDocumentDao doctorDocumentDao = new DoctorDocumentDao();
        doctorDocumentDao.setVerificationStatusForDocuments(List.of(seed.pendingDocumentId(), seed.secondPendingDocumentId()),
                "APPROVED");

        List<DoctorDocumentEntry> pending = doctorDocumentDao.findPendingDocumentsForAdmin();
        assertEquals(0, pending.size());
        assertEquals(0, doctorDocumentDao.countPendingDoctorsForAdmin());
    }
}
