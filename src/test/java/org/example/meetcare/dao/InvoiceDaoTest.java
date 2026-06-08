package org.example.meetcare.dao;

import org.example.meetcare.model.Invoice;
import org.example.meetcare.model.Users;
import org.example.meetcare.support.H2IntegrationTest;
import org.example.meetcare.utils.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceDaoTest extends H2IntegrationTest {
    private final InvoiceDao invoiceDao = new InvoiceDao();

    @Test
    void findInvoicesShouldReturnDoctorAndPatientViews() {
        List<Invoice> doctorInvoices = invoiceDao.findByDoctorUserId(seed.verifiedDoctorUserId());
        List<Invoice> patientInvoices = invoiceDao.findByPatientUserId(seed.patientUserId());

        assertEquals(2, doctorInvoices.size());
        assertEquals(2, patientInvoices.size());
    }

    @Test
    void createInvoiceShouldPersistPendingInvoiceForLinkedDoctorProfile() {
        Invoice created = invoiceDao.createInvoice(seed.verifiedDoctorUserId(), seed.patientUserId(),
                BigDecimal.valueOf(120.50), "Consultation spécialisée");

        assertEquals(InvoiceStatus.PENDING, created.getStatus());
        assertEquals(seed.verifiedDoctorId(), created.getDoctorId());
        assertEquals(seed.patientUserId(), created.getPatientId());
    }

    @Test
    void markAsPaidShouldUpdateStatusAndPaymentMethod() {
        Invoice paid = invoiceDao.markAsPaid(seed.invoicePendingId(), "CARD");

        assertEquals(InvoiceStatus.PAID, paid.getStatus());
        assertEquals("CARD", paid.getPaymentMethod());
    }

    @Test
    void findEligiblePatientsShouldReturnUsersWithPatientRole() {
        List<Users> patients = invoiceDao.findEligiblePatients();

        assertEquals(1, patients.size());
        assertEquals(seed.patientUserId(), patients.get(0).getIdUsers());
    }
}
