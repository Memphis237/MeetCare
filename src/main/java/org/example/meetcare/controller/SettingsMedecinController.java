package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.DoctorAccountDao;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.DoctorAccountService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.LogoutUtil;

import java.util.Optional;

public class SettingsMedecinController {
    private final DoctorAccountService doctorAccountService = new DoctorAccountService();

    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitialsLabel;
    @FXML private Label accountNameLabel;
    @FXML private Label accountStatusLabel;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox emailNotifCheckBox;
    @FXML private CheckBox smsNotifCheckBox;
    @FXML private CheckBox newAppointmentsNotifCheckBox;
    @FXML private CheckBox cancellationsNotifCheckBox;
    @FXML private CheckBox messagesNotifCheckBox;

    @FXML
    private void initialize() {
        loadSettings();
    }

    @FXML
    private void handleSaveContact() {
        DoctorAccountDao.DoctorSettingsRecord settings = doctorAccountService.saveContactSettings(
                emailField.getText().trim(),
                phoneField.getText().trim()
        );
        fillForm(settings);
        showInfo("Informations de contact mises a jour.");
    }

    @FXML
    private void handleChangePassword() {
        doctorAccountService.updateDoctorPassword(
                currentPasswordField.getText(),
                newPasswordField.getText(),
                confirmPasswordField.getText()
        );
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showInfo("Mot de passe modifie.");
    }

    @FXML
    private void handleSaveNotifications() {
        DoctorAccountDao.DoctorSettingsRecord settings = doctorAccountService.saveNotificationSettings(
                emailNotifCheckBox.isSelected(),
                smsNotifCheckBox.isSelected(),
                newAppointmentsNotifCheckBox.isSelected(),
                cancellationsNotifCheckBox.isSelected(),
                messagesNotifCheckBox.isSelected()
        );
        fillForm(settings);
        showInfo("Preferences de notification enregistrees.");
    }

    @FXML
    private void handleDeactivateAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Desactivation du compte");
        alert.setHeaderText("Desactiver le compte professionnel ?");
        alert.setContentText("Vous serez redirige vers la connexion apres la desactivation.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        doctorAccountService.deactivateAccount();
        showInfo("Compte desactive.");
        LogoutUtil.logout(emailField);
    }

    private void loadSettings() {
        DoctorAccountDao.DoctorSettingsRecord settings = doctorAccountService.loadSettings();
        if (settings == null) {
            return;
        }
        fillForm(settings);
        refreshAvatar();
    }

    private void fillForm(DoctorAccountDao.DoctorSettingsRecord settings) {
        accountNameLabel.setText(settings.fullName());
        accountStatusLabel.setText("Statut du compte : " + settings.accountStatus());
        emailField.setText(settings.email());
        phoneField.setText(settings.phone());
        emailNotifCheckBox.setSelected(settings.notifyEmail());
        smsNotifCheckBox.setSelected(settings.notifySms());
        newAppointmentsNotifCheckBox.setSelected(settings.notifyNewAppointments());
        cancellationsNotifCheckBox.setSelected(settings.notifyCancellations());
        messagesNotifCheckBox.setSelected(settings.notifyNewMessages());
    }

    private void refreshAvatar() {
        Users user = AppSession.getCurrentUser();
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
