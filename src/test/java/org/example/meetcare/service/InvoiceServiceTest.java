package org.example.meetcare.service;

import org.example.meetcare.model.Invoice;
import org.example.meetcare.model.Users;
import org.example.meetcare.support.H2IntegrationTest;
import org.example.meetcare.utils.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceServiceTest extends H2IntegrationTest {
    private final InvoiceService invoiceService = new InvoiceService();

    @Test
    void loadInvoicesShouldReturnSeededData() {
        Users doctor = new Users();
        doctor.setIdUsers(seed.verifiedDoctorUserId());
        Users patient = new Users();
        patient.setIdUsers(seed.patientUserId());

        List<Invoice> doctorInvoices = invoiceService.loadDoctorInvoices(doctor);
        List<Invoice> patientInvoices = invoiceService.loadPatientInvoices(patient);

        assertEquals(2, doctorInvoices.size());
        assertEquals(2, patientInvoices.size());
    }

    @Test
    void createInvoiceShouldReturnPersistedPendingInvoice() {
        Users doctor = new Users();
        doctor.setIdUsers(seed.verifiedDoctorUserId());
        Users patient = new Users();
        patient.setIdUsers(seed.patientUserId());

        Invoice invoice = invoiceService.createInvoice(doctor, patient, BigDecimal.valueOf(95), "Contrôle");

        assertEquals(InvoiceStatus.PENDING, invoice.getStatus());
        assertEquals(seed.verifiedDoctorId(), invoice.getDoctorId());
    }

    @Test
    void payInvoiceShouldMarkInvoiceAsPaid() {
        Invoice invoice = invoiceService.payInvoice(seed.invoicePendingId(), "ONLINE");

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals("ONLINE", invoice.getPaymentMethod());
    }
}
