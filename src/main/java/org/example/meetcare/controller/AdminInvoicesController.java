package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class AdminInvoicesController {
    @FXML private Label totalInvoicesLabel;
    @FXML private Label pendingInvoicesLabel;
    @FXML private Label paidInvoicesLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private VBox invoicesContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("invoices");
        statusFilterComboBox.getItems().setAll("Tous les statuts", "PAYEE", "EN_ATTENTE");
        statusFilterComboBox.setValue("Tous les statuts");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshInvoices());
        statusFilterComboBox.setOnAction(event -> refreshInvoices());
        refreshInvoices();
    }

    private void refreshInvoices() {
        List<AdminWorkspaceDao.AdminInvoiceRecord> allInvoices = adminWorkspaceDao.getInvoices();
        totalInvoicesLabel.setText(String.valueOf(allInvoices.size()));
        pendingInvoicesLabel.setText(String.valueOf(allInvoices.stream().filter(invoice -> isPending(invoice.status())).count()));
        paidInvoicesLabel.setText(String.valueOf(allInvoices.stream().filter(invoice -> isPaid(invoice.status())).count()));

        String query = normalize(searchField.getText());
        String status = statusFilterComboBox.getValue();
        List<AdminWorkspaceDao.AdminInvoiceRecord> filtered = allInvoices.stream()
                .filter(invoice -> query.isBlank()
                        || normalize(invoice.patientName()).contains(query)
                        || normalize(invoice.description()).contains(query))
                .filter(invoice -> "Tous les statuts".equals(status) || invoice.status().equalsIgnoreCase(status))
                .toList();

        invoicesContainer.getChildren().clear();
        for (AdminWorkspaceDao.AdminInvoiceRecord invoice : filtered) {
            invoicesContainer.getChildren().add(buildInvoiceCard(invoice));
        }
    }

    private VBox buildInvoiceCard(AdminWorkspaceDao.AdminInvoiceRecord invoice) {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("admin-list-item");

        HBox row = new HBox(18);
        VBox details = new VBox(6);
        Label title = new Label(invoice.description());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        Label patient = new Label("Patient: " + invoice.patientName());
        patient.getStyleClass().add("admin-muted");
        Label meta = new Label("Paiement: " + invoice.paymentMethod() + "  •  Date: " + (invoice.paidAt().isBlank() ? "En attente" : invoice.paidAt()));
        meta.getStyleClass().add("admin-muted");
        details.getChildren().addAll(title, patient, meta);
        HBox.setHgrow(details, Priority.ALWAYS);

        VBox amountBox = new VBox(8);
        Label amount = new Label(formatAmount(invoice.amount()));
        amount.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #111827;");
        Label badge = new Label(invoice.status());
        badge.getStyleClass().addAll("admin-badge", isPaid(invoice.status()) ? "admin-badge-active" : "admin-badge-pending");
        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().add("admin-danger-button");
        deleteButton.setOnAction(event -> {
            adminWorkspaceDao.deleteInvoice(invoice.id());
            refreshInvoices();
        });
        amountBox.getChildren().addAll(amount, badge, deleteButton);

        row.getChildren().addAll(details, amountBox);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private boolean isPaid(String status) {
        return "PAYEE".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);
    }

    private boolean isPending(String status) {
        return !isPaid(status);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString() + " €";
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.FRENCH).trim();
    }
}
