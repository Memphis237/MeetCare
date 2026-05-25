package org.example.meetcare.dao;

import org.example.meetcare.model.Invoice;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.DatabaseConnection;
import org.example.meetcare.utils.InvoiceStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceDao {
    public List<Invoice> findByDoctorUserId(int doctorUserId) {
        String sql = """
                SELECT i.id_invoices, i.amount, i.description, i.status, i.payment_method, i.paid_at, i.date,
                       i.id_doctors, i.id_users,
                       du.username AS doctor_username, d.speciality AS doctor_speciality,
                       pu.prenom AS patient_prenom, pu.username AS patient_username
                FROM invoices i
                JOIN doctors d ON d.id_doctors = i.id_doctors
                JOIN users du ON du.id_users = d.id_users
                JOIN users pu ON pu.id_users = i.id_users
                WHERE d.id_users = ?
                ORDER BY i.date DESC, i.id_invoices DESC
                """;

        List<Invoice> invoices = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, doctorUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invoices.add(mapInvoice(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les factures du medecin.", e);
        }
        return invoices;
    }

    public List<Invoice> findByPatientUserId(int patientUserId) {
        String sql = """
                SELECT i.id_invoices, i.amount, i.description, i.status, i.payment_method, i.paid_at, i.date,
                       i.id_doctors, i.id_users,
                       du.username AS doctor_username, d.speciality AS doctor_speciality,
                       pu.prenom AS patient_prenom, pu.username AS patient_username
                FROM invoices i
                JOIN doctors d ON d.id_doctors = i.id_doctors
                JOIN users du ON du.id_users = d.id_users
                JOIN users pu ON pu.id_users = i.id_users
                WHERE i.id_users = ?
                ORDER BY i.date DESC, i.id_invoices DESC
                """;

        List<Invoice> invoices = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invoices.add(mapInvoice(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les factures du patient.", e);
        }
        return invoices;
    }

    public List<Users> findEligiblePatients() {
        String sql = """
                SELECT id_users, username, prenom, email
                FROM users
                WHERE role = 'USER'
                ORDER BY prenom, username
                """;

        List<Users> patients = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Users user = new Users();
                user.setIdUsers(resultSet.getInt("id_users"));
                user.setUsername(resultSet.getString("username"));
                user.setPrenom(resultSet.getString("prenom"));
                user.setEmail(resultSet.getString("email"));
                patients.add(user);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les patients.", e);
        }
        return patients;
    }

    public Invoice createInvoice(int doctorUserId, int patientUserId, BigDecimal amount, String description) {
        int doctorId = findDoctorIdByUserId(doctorUserId)
                .orElseThrow(() -> new IllegalStateException("Le medecin courant n'est pas lie a un profil medecin."));

        String sql = """
                INSERT INTO invoices (amount, description, status, payment_method, paid_at, id_doctors, id_users)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setBigDecimal(1, amount);
            statement.setString(2, description);
            statement.setString(3, InvoiceStatus.PENDING);
            statement.setNull(4, Types.VARCHAR);
            statement.setNull(5, Types.TIMESTAMP);
            statement.setInt(6, doctorId);
            statement.setInt(7, patientUserId);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1))
                            .orElseThrow(() -> new IllegalStateException("Facture creee mais introuvable."));
                }
            }
            throw new IllegalStateException("Impossible de creer la facture.");
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer la facture.", e);
        }
    }

    public Invoice markAsPaid(int invoiceId, String paymentMethod) {
        String sql = """
                UPDATE invoices
                SET status = ?, payment_method = ?, paid_at = ?
                WHERE id_invoices = ?
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, InvoiceStatus.PAID);
            statement.setString(2, paymentMethod);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(4, invoiceId);
            statement.executeUpdate();
            return findById(invoiceId)
                    .orElseThrow(() -> new IllegalStateException("Facture introuvable apres paiement."));
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de marquer la facture comme payee.", e);
        }
    }

    public Optional<Invoice> findById(int invoiceId) {
        String sql = """
                SELECT i.id_invoices, i.amount, i.description, i.status, i.payment_method, i.paid_at, i.date,
                       i.id_doctors, i.id_users,
                       du.username AS doctor_username, d.speciality AS doctor_speciality,
                       pu.prenom AS patient_prenom, pu.username AS patient_username
                FROM invoices i
                JOIN doctors d ON d.id_doctors = i.id_doctors
                JOIN users du ON du.id_users = d.id_users
                JOIN users pu ON pu.id_users = i.id_users
                WHERE i.id_invoices = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapInvoice(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de recuperer la facture.", e);
        }
    }

    public Optional<Integer> findDoctorIdByUserId(int doctorUserId) {
        String sql = "SELECT id_doctors FROM doctors WHERE id_users = ? LIMIT 1";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, doctorUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("id_doctors"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de recuperer le profil medecin.", e);
        }
    }

    private Invoice mapInvoice(ResultSet resultSet) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(resultSet.getInt("id_invoices"));
        invoice.setAmount(resultSet.getBigDecimal("amount"));
        invoice.setDescription(resultSet.getString("description"));
        invoice.setStatus(resultSet.getString("status"));
        invoice.setPaymentMethod(resultSet.getString("payment_method"));
        invoice.setDoctorId(resultSet.getInt("id_doctors"));
        invoice.setPatientId(resultSet.getInt("id_users"));
        invoice.setDoctorName(resultSet.getString("doctor_username"));
        invoice.setDoctorSpeciality(resultSet.getString("doctor_speciality"));
        invoice.setPatientName(buildPatientName(resultSet.getString("patient_prenom"), resultSet.getString("patient_username")));

        Timestamp paidAt = resultSet.getTimestamp("paid_at");
        if (paidAt != null) {
            invoice.setPaidAt(paidAt.toLocalDateTime());
        }

        Timestamp createdAt = resultSet.getTimestamp("date");
        if (createdAt != null) {
            invoice.setCreatedAt(createdAt.toLocalDateTime());
        }

        return invoice;
    }

    private String buildPatientName(String prenom, String username) {
        String first = prenom == null ? "" : prenom.trim();
        String second = username == null ? "" : username.trim();

        if (first.isEmpty() && second.isEmpty()) {
            return "Patient";
        }
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + " " + second;
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }
}
