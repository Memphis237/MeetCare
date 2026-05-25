package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;
import org.example.meetcare.model.MedicalDocumentData;
import org.example.meetcare.service.MedicalDocumentFormService;
import org.example.meetcare.utils.NavigationUtil;
import org.example.meetcare.utils.SelectedPatientStore;

import java.math.BigDecimal;
import java.util.Optional;

public class PatientDetailMedecinController {
    @FXML private Label initialsLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label patientContactLabel;
    @FXML private Label patientBirthLabel;
    @FXML private Label patientAddressLabel;
    @FXML private Button historyTabButton;
    @FXML private Button documentsTabButton;
    @FXML private Button invoicesTabButton;
    @FXML private Button notesTabButton;
    @FXML private VBox historySection;
    @FXML private VBox documentsSection;
    @FXML private VBox invoicesSection;
    @FXML private VBox notesSection;
    @FXML private VBox historyContainer;
    @FXML private VBox documentsContainer;
    @FXML private VBox invoicesContainer;
    @FXML private VBox notesContainer;
    @FXML private TextArea noteInputArea;
    @FXML private Button actionAnchorButton;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();
    private DoctorWorkspaceDao.PatientDetail detail;

    @FXML
    private void initialize() {
        detail = doctorWorkspaceDao.getPatientDetail(SelectedPatientStore.getPatientId());
        if (detail == null) {
            return;
        }

        DoctorWorkspaceDao.PatientRecord patient = detail.patient();
        initialsLabel.setText(patient.initials());
        patientNameLabel.setText(patient.name());
        patientContactLabel.setText(patient.email() + " - " + patient.phone());
        patientBirthLabel.setText("Date de naissance : " + patient.birthDate());
        patientAddressLabel.setText(patient.address());

        renderHistory();
        renderDocuments();
        renderInvoices();
        renderNotes();
        showTab(SelectedPatientStore.getInitialTab());
    }

    @FXML
    private void handleBackToList() {
        NavigationUtil.navigateTo(actionAnchorButton, "/view/patientsMedecin.fxml", 1200, 760, "MeetCare - Patients");
    }

    @FXML
    private void handleShowHistory() {
        showTab("history");
    }

    @FXML
    private void handleShowDocuments() {
        showTab("documents");
    }

    @FXML
    private void handleShowInvoices() {
        showTab("invoices");
    }

    @FXML
    private void handleShowNotes() {
        showTab("notes");
    }

    @FXML
    private void handleSendDocument() {
        Optional<MedicalDocumentData> result = MedicalDocumentFormService.showDocumentDialog(detail.patient().name());
        result.ifPresent(document -> {
            doctorWorkspaceDao.insertMedicalDocument(detail.patient().id(), document);
            reload("documents");
        });
    }

    @FXML
    private void handleCreateInvoice() {
        TextInputDialog dialog = new TextInputDialog("25");
        dialog.setTitle("Creer facture");
        dialog.setHeaderText("Nouvelle facture pour " + detail.patient().name());
        dialog.setContentText("Montant (EUR) :");
        dialog.showAndWait().ifPresent(value -> {
            try {
                BigDecimal amount = new BigDecimal(value.trim());
                doctorWorkspaceDao.insertInvoice(detail.patient().id(), amount);
                reload("invoices");
            } catch (Exception ignored) {
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message");
        alert.setHeaderText(detail.patient().name());
        alert.setContentText("La messagerie detaillee pourra etre branchee ici.");
        alert.showAndWait();
    }

    @FXML
    private void handleAddNote() {
        String content = noteInputArea.getText();
        if (content == null || content.isBlank()) {
            return;
        }
        doctorWorkspaceDao.insertNote(detail.patient().id(), content.trim());
        noteInputArea.clear();
        reload("notes");
    }

    private void reload(String tab) {
        detail = doctorWorkspaceDao.getPatientDetail(detail.patient().id());
        renderHistory();
        renderDocuments();
        renderInvoices();
        renderNotes();
        showTab(tab);
    }

    private void renderHistory() {
        historyContainer.getChildren().clear();
        if (detail.appointments().isEmpty()) {
            historyContainer.getChildren().add(buildEmptyState("Aucun rendez-vous pour le moment"));
            return;
        }

        for (DoctorWorkspaceDao.AppointmentHistoryRecord item : detail.appointments()) {
            HBox row = new HBox(16);
            row.getStyleClass().add("doctor-list-row");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label title = new Label(item.title());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            Label date = new Label(item.date());
            date.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            info.getChildren().addAll(title, date);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(item.status());
            status.getStyleClass().add(resolveStatusStyle(item.status()));

            row.getChildren().addAll(info, spacer, status);
            historyContainer.getChildren().add(row);
        }
    }

    private void renderDocuments() {
        documentsContainer.getChildren().clear();
        if (detail.documents().isEmpty()) {
            documentsContainer.getChildren().add(buildEmptyState("Aucun document envoye pour le moment"));
            return;
        }

        for (DoctorWorkspaceDao.DocumentHistoryRecord item : detail.documents()) {
            HBox row = new HBox(16);
            row.getStyleClass().add("doctor-list-row");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label title = new Label(item.title());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            Label date = new Label(item.date());
            date.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            info.getChildren().addAll(title, date);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label type = new Label(item.type());
            type.getStyleClass().add("doctor-badge-outline");

            row.getChildren().addAll(info, spacer, type);
            documentsContainer.getChildren().add(row);
        }
    }

    private void renderInvoices() {
        invoicesContainer.getChildren().clear();
        if (detail.invoices().isEmpty()) {
            invoicesContainer.getChildren().add(buildEmptyState("Aucune facture pour le moment"));
            return;
        }

        for (DoctorWorkspaceDao.InvoiceHistoryRecord item : detail.invoices()) {
            HBox row = new HBox(16);
            row.getStyleClass().add("doctor-list-row");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label title = new Label(item.title());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            Label date = new Label(item.date());
            date.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            info.getChildren().addAll(title, date);

            Label amount = new Label(item.amount());
            amount.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            Label status = new Label(item.status());
            status.getStyleClass().add(resolveStatusStyle(item.status()));

            row.getChildren().addAll(info, amount, status);
            invoicesContainer.getChildren().add(row);
        }
    }

    private void renderNotes() {
        notesContainer.getChildren().clear();
        if (detail.notes().isEmpty()) {
            notesContainer.getChildren().add(buildEmptyState("Aucune note medicale pour le moment"));
            return;
        }

        for (DoctorWorkspaceDao.NoteHistoryRecord item : detail.notes()) {
            VBox row = new VBox(6);
            row.getStyleClass().add("doctor-list-row");

            Label date = new Label(item.date());
            date.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Label content = new Label(item.content());
            content.setWrapText(true);
            content.setStyle("-fx-font-size: 14px; -fx-text-fill: #0f172a;");

            row.getChildren().addAll(date, content);
            notesContainer.getChildren().add(row);
        }
    }

    private VBox buildEmptyState(String message) {
        VBox box = new VBox(6);
        box.getStyleClass().add("doctor-list-row");

        Label title = new Label(message);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Cette section se remplira automatiquement au fur et a mesure.");
        subtitle.getStyleClass().add("doctor-subtitle");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private String resolveStatusStyle(String status) {
        if ("En attente".equalsIgnoreCase(status)) {
            return "doctor-badge-orange";
        }
        if ("Annule".equalsIgnoreCase(status)) {
            return "doctor-badge-neutral";
        }
        return "doctor-badge-green";
    }

    private void showTab(String tab) {
        historySection.setManaged("history".equals(tab));
        historySection.setVisible("history".equals(tab));
        documentsSection.setManaged("documents".equals(tab));
        documentsSection.setVisible("documents".equals(tab));
        invoicesSection.setManaged("invoices".equals(tab));
        invoicesSection.setVisible("invoices".equals(tab));
        notesSection.setManaged("notes".equals(tab));
        notesSection.setVisible("notes".equals(tab));

        setTabStyle(historyTabButton, "history".equals(tab));
        setTabStyle(documentsTabButton, "documents".equals(tab));
        setTabStyle(invoicesTabButton, "invoices".equals(tab));
        setTabStyle(notesTabButton, "notes".equals(tab));
    }

    private void setTabStyle(Button button, boolean active) {
        button.getStyleClass().removeAll("doctor-tab-active", "doctor-tab-button");
        button.getStyleClass().add(active ? "doctor-tab-active" : "doctor-tab-button");
    }
}
