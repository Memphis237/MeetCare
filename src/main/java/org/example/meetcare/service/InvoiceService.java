package org.example.meetcare.service;

import org.example.meetcare.dao.InvoiceDao;
import org.example.meetcare.model.Invoice;
import org.example.meetcare.model.Users;

import java.math.BigDecimal;
import java.util.List;

public class InvoiceService {
    private final InvoiceDao invoiceDao = new InvoiceDao();

    public List<Invoice> loadDoctorInvoices(Users doctorUser) {
        requireUser(doctorUser);
        return invoiceDao.findByDoctorUserId(doctorUser.getIdUsers());
    }

    public List<Invoice> loadPatientInvoices(Users patientUser) {
        requireUser(patientUser);
        return invoiceDao.findByPatientUserId(patientUser.getIdUsers());
    }

    public List<Users> loadEligiblePatients() {
        return invoiceDao.findEligiblePatients();
    }

    public Invoice createInvoice(Users doctorUser, Users patientUser, BigDecimal amount, String description) {
        requireUser(doctorUser);
        requireUser(patientUser);
        return invoiceDao.createInvoice(doctorUser.getIdUsers(), patientUser.getIdUsers(), amount, description);
    }

    public Invoice payInvoice(int invoiceId, String paymentMethod) {
        return invoiceDao.markAsPaid(invoiceId, paymentMethod);
    }

    private void requireUser(Users user) {
        if (user == null || user.getIdUsers() <= 0) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }
    }
}
