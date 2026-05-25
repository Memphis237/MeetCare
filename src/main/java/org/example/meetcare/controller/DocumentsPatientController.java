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
import org.example.meetcare.dao.PatientCareDao;

import java.util.ArrayList;
import java.util.List;

public class DocumentsPatientController {
    private static final String ALL_DOCTORS = "Tous les medecins";
    private static final String ALL_TYPES = "Tous les types";

    @FXML private ComboBox<String> doctorFilterComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private Label documentsCountLabel;
    @FXML private VBox documentsContainer;

    private final PatientCareDao patientCareDao = new PatientCareDao();
    private List<PatientCareDao.PatientDocumentRecord> allDocuments = List.of();

    @FXML
    private void initialize() {
        reloadDocuments();
        doctorFilterComboBox.setOnAction(event -> refreshDocuments());
        typeFilterComboBox.setOnAction(event -> refreshDocuments());
    }

    @FXML
    private void handleImportDocument() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText("Importer un document");
        alert.setContentText("Cette fonctionnalite reste reservee au parcours patient. Les documents envoyes par le medecin apparaitront automatiquement ici.");
        alert.showAndWait();
    }

    private void reloadDocuments() {
        allDocuments = patientCareDao.getPatientDocuments();
        populateFilters();
        refreshDocuments();
    }

    private void populateFilters() {
        List<String> doctorOptions = new ArrayList<>();
        doctorOptions.add(ALL_DOCTORS);
        allDocuments.stream()
                .map(PatientCareDao.PatientDocumentRecord::doctorName)
                .distinct()
                .sorted()
                .forEach(doctorOptions::add);

        List<String> typeOptions = new ArrayList<>();
        typeOptions.add(ALL_TYPES);
        allDocuments.stream()
                .map(PatientCareDao.PatientDocumentRecord::documentType)
                .distinct()
                .sorted()
                .forEach(typeOptions::add);

        doctorFilterComboBox.setItems(FXCollections.observableArrayList(doctorOptions));
        typeFilterComboBox.setItems(FXCollections.observableArrayList(typeOptions));
        doctorFilterComboBox.setValue(ALL_DOCTORS);
        typeFilterComboBox.setValue(ALL_TYPES);
    }

    private void refreshDocuments() {
        String selectedDoctor = doctorFilterComboBox.getValue();
        String selectedType = typeFilterComboBox.getValue();

        List<PatientCareDao.PatientDocumentRecord> filteredDocuments = allDocuments.stream()
                .filter(document -> selectedDoctor == null || ALL_DOCTORS.equals(selectedDoctor) || document.doctorName().equals(selectedDoctor))
                .filter(document -> selectedType == null || ALL_TYPES.equals(selectedType) || document.documentType().equals(selectedType))
                .toList();

        documentsContainer.getChildren().clear();
        documentsCountLabel.setText(filteredDocuments.size() + (filteredDocuments.size() > 1 ? " document(s)" : " document"));

        if (filteredDocuments.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.getStyleClass().add("document-card");

            Label title = new Label("Aucun document trouve");
            title.getStyleClass().add("document-title");

            Label subtitle = new Label("Les documents apparaissent ici uniquement lorsqu'un medecin te les a envoyes.");
            subtitle.getStyleClass().add("doctor-info");

            emptyState.getChildren().addAll(title, subtitle);
            documentsContainer.getChildren().add(emptyState);
            return;
        }

        for (PatientCareDao.PatientDocumentRecord document : filteredDocuments) {
            documentsContainer.getChildren().add(createDocumentCard(document));
        }
    }

    private HBox createDocumentCard(PatientCareDao.PatientDocumentRecord document) {
        HBox card = new HBox(20);
        card.getStyleClass().add("document-card");
        card.setFillHeight(true);

        Label icon = new Label("DOC");
        icon.getStyleClass().add("doc-icon");

        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(document.title());
        title.getStyleClass().add("document-title");

        Label doctor = new Label(document.doctorName() + " - " + document.speciality());
        doctor.getStyleClass().add("doctor-info");

        Label description = new Label(document.description());
        description.setWrapText(true);
        description.getStyleClass().add("doctor-info");

        info.getChildren().addAll(title, doctor, description);

        Label date = new Label(document.date());
        date.getStyleClass().add("doctor-info");

        Label typeBadge = new Label(document.documentType());
        typeBadge.getStyleClass().add(resolveBadgeStyle(document.documentType()));

        Button downloadButton = new Button("Voir le detail");
        downloadButton.getStyleClass().add("secondary-button");
        downloadButton.setOnAction(event -> showDetails(document));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(icon, info, spacer, date, typeBadge, downloadButton);
        return card;
    }

    private String resolveBadgeStyle(String type) {
        return switch (type) {
            case "Ordonnance" -> "badge-blue";
            case "Compte rendu" -> "badge-purple";
            case "Analyse" -> "badge-green";
            case "Radio" -> "badge-orange";
            default -> "badge";
        };
    }

    private void showDetails(PatientCareDao.PatientDocumentRecord document) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(document.documentType());
        alert.setHeaderText(document.title());
        alert.setContentText(document.description());
        alert.showAndWait();
    }
}
