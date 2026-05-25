package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;

import java.math.BigDecimal;
import java.util.List;

public class InvoiceMedecinController {
    @FXML private Label totalInvoicesValueLabel;
    @FXML private Label totalRevenueValueLabel;
    @FXML private Label pendingRevenueValueLabel;
    @FXML private Label paidInvoicesValueLabel;
    @FXML private VBox invoicesContainer;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        List<DoctorWorkspaceDao.InvoiceRecord> invoices = doctorWorkspaceDao.getInvoices();
        totalInvoicesValueLabel.setText(String.valueOf(invoices.size()));

        BigDecimal paidTotal = invoices.stream()
                .filter(invoice -> "Payee".equals(invoice.status()))
                .map(invoice -> new BigDecimal(invoice.amount().replace(" EUR", "")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingTotal = invoices.stream()
                .filter(invoice -> "En attente".equals(invoice.status()))
                .map(invoice -> new BigDecimal(invoice.amount().replace(" EUR", "")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidCount = invoices.stream().filter(invoice -> "Payee".equals(invoice.status())).count();

        totalRevenueValueLabel.setText(paidTotal.stripTrailingZeros().toPlainString() + " EUR");
        pendingRevenueValueLabel.setText(pendingTotal.stripTrailingZeros().toPlainString() + " EUR");
        paidInvoicesValueLabel.setText(String.valueOf(paidCount));

        invoicesContainer.getChildren().clear();
        if (invoices.isEmpty()) {
            VBox emptyCard = new VBox(6);
            emptyCard.getStyleClass().add("doctor-card");
            Label title = new Label("Aucune facture pour le moment");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label subtitle = new Label("Les factures apparaitront ici au fur et a mesure des consultations facturees.");
            subtitle.getStyleClass().add("doctor-subtitle");
            emptyCard.getChildren().addAll(title, subtitle);
            invoicesContainer.getChildren().add(emptyCard);
            return;
        }

        for (DoctorWorkspaceDao.InvoiceRecord invoice : invoices) {
            HBox row = new HBox(16);
            row.getStyleClass().add("doctor-list-row");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label patient = new Label(invoice.patientName());
            patient.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label description = new Label(invoice.description() + " - " + invoice.date());
            description.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            info.getChildren().addAll(patient, description);

            Label amount = new Label(invoice.amount());
            amount.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label status = new Label(invoice.status());
            status.getStyleClass().add("Payee".equals(invoice.status()) ? "doctor-badge-green" : "doctor-badge-orange");

            row.getChildren().addAll(info, amount, status);
            invoicesContainer.getChildren().add(row);
        }
    }
}
