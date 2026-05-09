package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.dao.UsersDao;

import java.io.IOException;

public class HomePatientController {
    private final UsersDao usersDao = new UsersDao();

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private void initialize() {
        Users user = AppSession.getCurrentUser();
        if (user == null) {
            return;
        }

        String displayName = user.getUsername() == null || user.getUsername().isBlank() ? "Patient" : user.getUsername();
        patientNameLabel.setText(displayName);
        welcomeLabel.setText("Bienvenue, " + displayName);
        roleLabel.setText(roleToLabel(user.getRole()));
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        Users user = AppSession.getCurrentUser();
        try {
            if (user != null) {
                usersDao.updateOnlineStatus(user.getIdUsers(), false);
            }
        } catch (RuntimeException ignored) {
            // On continue la déconnexion locale même si la base répond mal.
        }
        AppSession.clear();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 620, 520);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("MeetCare - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de revenir à la page de connexion.", ex);
        }
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        navigateTo(event, "/view/settingsPatient.fxml", 1100, 700, "MeetCare - Paramètres");
    }

    private void navigateTo(MouseEvent event, String fxmlPath, double width, double height, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'ouvrir la page demandée.", ex);
        }
    }

    private String roleToLabel(Role role) {
        if (role == null) {
            return "Patient";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Médecin";
            default -> "Patient";
        };
    }
}
