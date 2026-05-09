package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.RegisterService;

import java.io.IOException;

public class RegisterController {
    @FXML
    private TextField prenom;

    @FXML
    private TextField username;

    @FXML
    private TextField email;

    @FXML
    private TextField telephone;

    @FXML
    private PasswordField password;

    @FXML
    private PasswordField confirmPassword;

    @FXML
    private Label messageLabel;

    private final RegisterService registerService = new RegisterService();

    @FXML
    private void handleInscription(ActionEvent event) {
        try {
            Users user = new Users();
            user.setUsername(isBlank(username.getText()) ? prenom.getText() : username.getText());
            user.setEmail(email.getText());
            user.setPhone(telephone.getText());
            user.setPassword(password.getText());

            registerService.register(user, confirmPassword.getText());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Inscription réussie");
            alert.setHeaderText(null);
            alert.setContentText("Compte créé avec succès. Vous pouvez maintenant vous connecter.");
            alert.showAndWait();
            navigate(event, "/view/login.fxml");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), false);
        } catch (Exception ex) {
            showMessage("Une erreur est survenue pendant l'inscription.", false);
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        navigate(event, "/view/login.fxml");
    }

    private void navigate(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
            showMessage("Impossible d'ouvrir la page demandée.", false);
        }
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success
                ? "-fx-text-fill: #1b8f4b; -fx-font-size: 13px;"
                : "-fx-text-fill: #d64545; -fx-font-size: 13px;");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
