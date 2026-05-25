package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

import java.util.List;
import java.util.Locale;

public class AdminDoctorsController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label doctorsCountLabel;
    @FXML private VBox doctorsContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("doctors");
        statusFilterComboBox.getItems().setAll("Tous les statuts", "Valide", "En attente", "Inactif");
        statusFilterComboBox.setValue("Tous les statuts");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshDoctors());
        statusFilterComboBox.setOnAction(event -> refreshDoctors());
        refreshDoctors();
    }

    private void refreshDoctors() {
        String query = normalize(searchField.getText());
        String status = statusFilterComboBox.getValue();

        List<AdminWorkspaceDao.AdminDoctorRecord> doctors = adminWorkspaceDao.getDoctors().stream()
                .filter(doctor -> query.isBlank()
                        || normalize(doctor.name()).contains(query)
                        || normalize(doctor.speciality()).contains(query))
                .filter(doctor -> "Tous les statuts".equals(status) || doctor.status().equalsIgnoreCase(status))
                .toList();

        doctorsCountLabel.setText(doctors.size() + (doctors.size() > 1 ? " médecin(s)" : " médecin"));
        doctorsContainer.getChildren().clear();
        for (AdminWorkspaceDao.AdminDoctorRecord doctor : doctors) {
            doctorsContainer.getChildren().add(buildDoctorCard(doctor));
        }
    }

    private VBox buildDoctorCard(AdminWorkspaceDao.AdminDoctorRecord doctor) {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("admin-list-item");

        HBox row = new HBox(16);
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("admin-avatar");
        Label initials = new Label(doctor.initials());
        initials.getStyleClass().add("admin-avatar-label");
        avatar.getChildren().add(initials);

        VBox details = new VBox(6);
        HBox top = new HBox(10);
        Label name = new Label(doctor.name());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        Label statusBadge = new Label(doctor.status());
        statusBadge.getStyleClass().addAll("admin-badge", badgeForStatus(doctor.status()));
        top.getChildren().addAll(name, statusBadge);
        Label subtitle = new Label(doctor.speciality() + " • " + doctor.city());
        subtitle.getStyleClass().add("admin-muted");
        HBox tags = new HBox(8);
        tags.getChildren().addAll(
                buildTag(doctor.sector(), "admin-badge-info"),
                buildTag(doctor.acceptsVitale() ? "Carte Vitale" : "Sans Vitale", doctor.acceptsVitale() ? "admin-badge-active" : "admin-badge-danger"),
                buildTag(doctor.teleconsultation() ? "Téléconsultation" : "Cabinet", doctor.teleconsultation() ? "admin-badge-purple" : "admin-badge-info")
        );
        details.getChildren().addAll(top, subtitle, tags);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button viewButton = buildButton("Voir profil", "admin-secondary-button");
        viewButton.setOnAction(event -> showDoctorProfile(doctor));
        actions.getChildren().add(viewButton);

        if ("En attente".equalsIgnoreCase(doctor.status())) {
            Button validateButton = buildButton("Valider", "admin-success-button");
            validateButton.setOnAction(event -> {
                adminWorkspaceDao.validateDoctor(doctor.doctorId());
                refreshDoctors();
            });
            Button refuseButton = buildButton("Refuser", "admin-danger-button");
            refuseButton.setOnAction(event -> {
                adminWorkspaceDao.refuseDoctor(doctor.doctorId());
                refreshDoctors();
            });
            actions.getChildren().addAll(validateButton, refuseButton);
        }

        Button deleteButton = buildButton("Supprimer", "admin-danger-button");
        deleteButton.setOnAction(event -> {
            adminWorkspaceDao.deleteDoctorUser(doctor.userId());
            refreshDoctors();
        });
        actions.getChildren().add(deleteButton);

        row.getChildren().addAll(avatar, details, actions);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private Label buildTag(String text, String styleClass) {
        Label tag = new Label(text);
        tag.getStyleClass().addAll("admin-badge", styleClass);
        return tag;
    }

    private void showDoctorProfile(AdminWorkspaceDao.AdminDoctorRecord doctor) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Profil médecin");
        alert.setHeaderText(doctor.name());
        alert.setContentText(
                "Email: " + doctor.email() + "\n" +
                "Spécialité: " + doctor.speciality() + "\n" +
                "Ville: " + doctor.city() + "\n" +
                "Secteur: " + doctor.sector() + "\n" +
                "Carte Vitale: " + (doctor.acceptsVitale() ? "Oui" : "Non") + "\n" +
                "Téléconsultation: " + (doctor.teleconsultation() ? "Oui" : "Non") + "\n" +
                "Statut: " + doctor.status()
        );
        alert.showAndWait();
    }

    private String badgeForStatus(String status) {
        return switch (status.toLowerCase(Locale.FRENCH)) {
            case "valide" -> "admin-badge-active";
            case "en attente" -> "admin-badge-pending";
            default -> "admin-badge-danger";
        };
    }

    private Button buildButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.FRENCH).trim();
    }
}
