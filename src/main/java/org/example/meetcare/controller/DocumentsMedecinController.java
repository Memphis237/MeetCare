package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;
import org.example.meetcare.model.MedicalDocumentData;
import org.example.meetcare.service.MedicalDocumentFormService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentsMedecinController {
    @FXML private ComboBox<String> patientFilterComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private Label documentsCountLabel;
    @FXML private VBox documentsContainer;
    @FXML private Button actionAnchorButton;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        populateFilters();
        patientFilterComboBox.setOnAction(event -> refreshDocuments());
        typeFilterComboBox.setOnAction(event -> refreshDocuments());
        refreshDocuments();
    }

    @FXML
    private void handleCreateDocument() {
        List<DoctorWorkspaceDao.PatientRecord> patients = doctorWorkspaceDao.getPatients();
        if (patients.isEmpty()) {
            showInfo("Aucun patient", "Ajoutez d'abord des rendez-vous avant de creer des documents.");
            return;
        }

        DoctorWorkspaceDao.PatientRecord patient = patients.stream()
                .filter(item -> item.name().equals(patientFilterComboBox.getValue()))
                .findFirst()
                .orElse(patients.get(0));

        Optional<MedicalDocumentData> result = MedicalDocumentFormService.showDocumentDialog(patient.name());
        result.ifPresent(document -> {
            doctorWorkspaceDao.insertMedicalDocument(patient.id(), document);
            initialize();
        });
    }

    private void populateFilters() {
        List<String> patients = new ArrayList<>();
        patients.add("Tous les patients");
        doctorWorkspaceDao.getDocuments().stream()
                .map(DoctorWorkspaceDao.DocumentRecord::patientName)
                .distinct()
                .sorted()
                .forEach(patients::add);

        List<String> types = new ArrayList<>();
        types.add("Tous les types");
        doctorWorkspaceDao.getDocuments().stream()
                .map(DoctorWorkspaceDao.DocumentRecord::type)
                .distinct()
                .sorted()
                .forEach(types::add);

        patientFilterComboBox.setItems(FXCollections.observableArrayList(patients));
        typeFilterComboBox.setItems(FXCollections.observableArrayList(types));
        patientFilterComboBox.setValue("Tous les patients");
        typeFilterComboBox.setValue("Tous les types");
    }

    private void refreshDocuments() {
        String patient = patientFilterComboBox.getValue();
        String type = typeFilterComboBox.getValue();

        List<DoctorWorkspaceDao.DocumentRecord> filtered = doctorWorkspaceDao.getDocuments().stream()
                .filter(document -> "Tous les patients".equals(patient) || document.patientName().equals(patient))
                .filter(document -> "Tous les types".equals(type) || document.type().equals(type))
                .toList();

        documentsCountLabel.setText(filtered.size() + (filtered.size() > 1 ? " document(s)" : " document"));
        documentsContainer.getChildren().clear();

        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.getStyleClass().add("doctor-card");

            Label title = new Label("Aucun document pour le moment");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label subtitle = new Label("Les documents apparaitront ici quand vous enverrez un document a un patient.");
            subtitle.getStyleClass().add("doctor-subtitle");

            emptyState.getChildren().addAll(title, subtitle);
            documentsContainer.getChildren().add(emptyState);
            return;
        }

        for (DoctorWorkspaceDao.DocumentRecord document : filtered) {
            HBox row = new HBox(16);
            row.getStyleClass().add("doctor-list-row");

            Label icon = new Label("DOC");
            icon.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-font-size: 13px; -fx-font-weight: bold; -fx-alignment: center; -fx-min-width: 44; -fx-min-height: 44; -fx-background-radius: 12;");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label title = new Label(document.title());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label patientName = new Label(document.patientName());
            patientName.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            Label description = new Label(document.description());
            description.setWrapText(true);
            description.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            info.getChildren().addAll(title, patientName, description);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label date = new Label(document.date());
            date.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

            Label typeBadge = new Label(document.type());
            typeBadge.getStyleClass().add("doctor-badge-outline");

            Label statusBadge = new Label(document.status());
            statusBadge.getStyleClass().add("Envoye".equals(document.status()) ? "doctor-badge-green" : "doctor-badge-neutral");

            row.getChildren().addAll(icon, info, spacer, date, typeBadge, statusBadge);
            documentsContainer.getChildren().add(row);
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
