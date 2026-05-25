package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

public class AdminSettingsController {
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox emailNotificationsCheckBox;
    @FXML private CheckBox smsNotificationsCheckBox;
    @FXML private CheckBox appointmentsNotificationsCheckBox;
    @FXML private CheckBox cancellationsNotificationsCheckBox;
    @FXML private CheckBox messagesNotificationsCheckBox;
    @FXML private Label feedbackLabel;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("settings");
        bindSettings();
    }

    @FXML
    private void handleSaveContact() {
        adminWorkspaceDao.updateAdminContact(emailField.getText().trim(), phoneField.getText().trim());
        showFeedback("Coordonnées mises à jour.", true);
    }

    @FXML
    private void handleSavePassword() {
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            showFeedback("La confirmation du mot de passe ne correspond pas.", false);
            return;
        }
        if (newPasswordField.getText().length() < 6) {
            showFeedback("Le nouveau mot de passe doit contenir au moins 6 caractères.", false);
            return;
        }
        boolean updated = adminWorkspaceDao.updateAdminPassword(currentPasswordField.getText(), newPasswordField.getText());
        if (!updated) {
            showFeedback("Le mot de passe actuel est incorrect.", false);
            return;
        }
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showFeedback("Mot de passe mis à jour.", true);
    }

    @FXML
    private void handleSaveNotifications() {
        adminWorkspaceDao.updateAdminNotifications(new AdminWorkspaceDao.SettingsRecord(
                emailField.getText().trim(),
                phoneField.getText().trim(),
                emailNotificationsCheckBox.isSelected(),
                smsNotificationsCheckBox.isSelected(),
                appointmentsNotificationsCheckBox.isSelected(),
                cancellationsNotificationsCheckBox.isSelected(),
                messagesNotificationsCheckBox.isSelected()
        ));
        showFeedback("Préférences enregistrées.", true);
    }

    @FXML
    private void handleShowAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare Admin");
        alert.setHeaderText("Configuration de la plateforme");
        alert.setContentText("Cet espace permet de gérer les paramètres généraux de l'administrateur connecté.");
        alert.showAndWait();
    }

    private void bindSettings() {
        AdminWorkspaceDao.SettingsRecord settings = adminWorkspaceDao.getAdminSettings();
        emailField.setText(settings.email());
        phoneField.setText(settings.phone());
        emailNotificationsCheckBox.setSelected(settings.notifyEmail());
        smsNotificationsCheckBox.setSelected(settings.notifySms());
        appointmentsNotificationsCheckBox.setSelected(settings.notifyNewAppointments());
        cancellationsNotificationsCheckBox.setSelected(settings.notifyCancellations());
        messagesNotificationsCheckBox.setSelected(settings.notifyNewMessages());
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setStyle(success
                ? "-fx-text-fill: #16a34a; -fx-font-size: 13px;"
                : "-fx-text-fill: #dc2626; -fx-font-size: 13px;");
    }
}
