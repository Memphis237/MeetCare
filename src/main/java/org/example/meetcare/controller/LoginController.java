package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.RegisterService;
import org.example.meetcare.utils.AppSession;

public class LoginController {
    @FXML
    private TextField username;

    @FXML
    private PasswordField password;

    @FXML
    private Label messageLabel;

    private final RegisterService authService = new RegisterService();
    private final UsersDao usersDao = new UsersDao();

    @FXML
    private void handleLogin(ActionEvent event) {
        try {
            Users user = authService.login(username.getText(), password.getText());
            Users freshUser = usersDao.findById(user.getIdUsers()).orElse(user);
            AppSession.setCurrentUser(freshUser);
            usersDao.updateOnlineStatus(freshUser.getIdUsers(), true);
            navigateAfterLogin(event, freshUser);
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), false);
        } catch (Exception ex) {
            ex.printStackTrace();
            showMessage("Une erreur est survenue pendant la connexion.", false);
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        navigate(event, "/view/register.fxml", 620, 520, "MeetCare - Inscription");
    }

    private void navigateAfterLogin(ActionEvent event, Users user) {
        Role role = user == null ? null : user.getRole();

        if (role == Role.ADMIN) {
            navigate(event, "/view/dashboard.fxml", 1200, 750, "MeetCare - Dashboard");
            return;
        }

        if (role == Role.MEDECIN) {
            navigate(event, "/view/homeMedecin.fxml", 1100, 700, "MeetCare - Espace medecin");
            return;
        }

        navigate(event, "/view/homePatient.fxml", 1100, 700, "MeetCare - Espace patient");
    }

    private void navigate(ActionEvent event, String fxmlPath, double width, double height, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception ex) {
            System.err.println("Erreur lors de la navigation vers " + fxmlPath + ":");
            ex.printStackTrace();
            showMessage("Impossible d'ouvrir la page demandee. " + rootCauseMessage(ex), false);
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success
                ? "-fx-text-fill: #1b8f4b; -fx-font-size: 13px;"
                : "-fx-text-fill: #d64545; -fx-font-size: 13px;");
    }
}
