package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;
import org.example.meetcare.utils.NavigationUtil;
import org.example.meetcare.utils.SelectedPatientStore;

import java.util.List;
import java.util.Locale;

public class PatientsMedecinController {
    @FXML private TextField searchField;
    @FXML private Label patientsCountLabel;
    @FXML private VBox patientsContainer;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshPatients());
        refreshPatients();
    }

    private void refreshPatients() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<DoctorWorkspaceDao.PatientRecord> patients = doctorWorkspaceDao.getPatients().stream()
                .filter(patient -> keyword.isBlank()
                        || patient.name().toLowerCase(Locale.ROOT).contains(keyword)
                        || patient.email().toLowerCase(Locale.ROOT).contains(keyword))
                .toList();

        patientsCountLabel.setText(patients.size() + (patients.size() > 1 ? " patient(s)" : " patient"));
        patientsContainer.getChildren().clear();

        if (patients.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.getStyleClass().add("doctor-card");

            Label title = new Label("Aucun patient pour le moment");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label subtitle = new Label("Votre liste se remplira automatiquement des qu'un patient prendra rendez-vous.");
            subtitle.getStyleClass().add("doctor-subtitle");

            emptyState.getChildren().addAll(title, subtitle);
            patientsContainer.getChildren().add(emptyState);
            return;
        }

        for (DoctorWorkspaceDao.PatientRecord patient : patients) {
            HBox row = new HBox(16);
            row.getStyleClass().add("doctor-list-row");

            Label avatar = new Label(patient.initials());
            avatar.getStyleClass().add("doctor-patient-avatar");

            VBox info = new VBox(6);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label name = new Label(patient.name());
            name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            Label contact = new Label(patient.email() + "  •  " + patient.phone());
            contact.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            Label meta = new Label(patient.appointmentCount() + " rendez-vous  •  Dernier rendez-vous: " + patient.lastVisit());
            meta.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
            info.getChildren().addAll(name, contact, meta);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button openButton = new Button("Voir dossier");
            openButton.getStyleClass().add("doctor-primary-button");
            openButton.setOnAction(event -> {
                SelectedPatientStore.setSelection(patient.id(), "history");
                NavigationUtil.navigateTo(openButton, "/view/patientDetailMedecin.fxml", 1200, 760, "MeetCare - Dossier patient");
            });

            row.getChildren().addAll(avatar, info, spacer, openButton);
            patientsContainer.getChildren().add(row);
        }
    }
}
