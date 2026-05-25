package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

public class AdminWaitlistController {
    @FXML private Label waitlistCountLabel;
    @FXML private VBox waitlistContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("waitlist");
        refreshWaitlist();
    }

    private void refreshWaitlist() {
        var doctors = adminWorkspaceDao.getDoctors().stream()
                .filter(doctor -> "En attente".equalsIgnoreCase(doctor.status()))
                .toList();
        waitlistCountLabel.setText(doctors.size() + (doctors.size() > 1 ? " dossier(s) en attente" : " dossier en attente"));
        waitlistContainer.getChildren().clear();
        for (AdminWorkspaceDao.AdminDoctorRecord doctor : doctors) {
            VBox card = new VBox();
            card.getStyleClass().add("admin-list-item");
            HBox row = new HBox(16);
            StackPane avatar = new StackPane();
            avatar.getStyleClass().add("admin-avatar");
            Label initials = new Label(doctor.initials());
            initials.getStyleClass().add("admin-avatar-label");
            avatar.getChildren().add(initials);

            VBox details = new VBox(6);
            Label name = new Label(doctor.name());
            name.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
            Label meta = new Label(doctor.speciality() + " • " + doctor.city() + " • " + doctor.email());
            meta.getStyleClass().add("admin-muted");
            details.getChildren().addAll(name, meta);
            HBox.setHgrow(details, Priority.ALWAYS);

            HBox actions = new HBox(10);
            Button validate = new Button("Valider");
            validate.getStyleClass().add("admin-success-button");
            validate.setOnAction(event -> {
                adminWorkspaceDao.validateDoctor(doctor.doctorId());
                refreshWaitlist();
            });
            Button refuse = new Button("Refuser");
            refuse.getStyleClass().add("admin-danger-button");
            refuse.setOnAction(event -> {
                adminWorkspaceDao.refuseDoctor(doctor.doctorId());
                refreshWaitlist();
            });
            actions.getChildren().addAll(validate, refuse);

            row.getChildren().addAll(avatar, details, actions);
            card.getChildren().add(row);
            waitlistContainer.getChildren().add(card);
        }
    }
}
