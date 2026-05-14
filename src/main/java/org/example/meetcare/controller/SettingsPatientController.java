package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.UserProfileService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;

public class SettingsPatientController {
    private final UserProfileService profileService = new UserProfileService();

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private Circle settingsAvatarCircle;

    @FXML
    private Label settingsAvatarInitialsLabel;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label profileRoleLabel;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField cityField;

    @FXML
    private TextField codePostalField;

    @FXML
    private TextField countryField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmNewPasswordField;

    @FXML
    private CheckBox emailNotifToggle;

    @FXML
    private CheckBox smsNotifToggle;

    @FXML
    private void initialize() {
        Users user = refreshCurrentUser();
        if (user == null) {
            return;
        }

        profileNameLabel.setText(buildDisplayName(user));
        profileRoleLabel.setText(roleToLabel(user.getRole()));
        prenomField.setText(user.getPrenom());
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        addressField.setText(user.getAddress());
        cityField.setText(user.getCity());
        codePostalField.setText(user.getCodePostal());
        countryField.setText(user.getCountry());
        if (emailNotifToggle != null) {
            emailNotifToggle.setSelected(true);
        }
        if (smsNotifToggle != null) {
            smsNotifToggle.setSelected(true);
        }
        refreshAvatars(user);
    }

    @FXML
    private void handleChangePhoto(ActionEvent event) {
        Users currentUser = refreshCurrentUser();
        Users updatedUser = profileService.updateAvatar(currentUser, (Node) event.getSource());
        AppSession.setCurrentUser(updatedUser);
        profileNameLabel.setText(buildDisplayName(updatedUser));
        refreshAvatars(updatedUser);
        showInfo("Photo de profil mise a jour.");
    }

    @FXML
    private void handleDeletePhoto(ActionEvent event) {
        Users currentUser = refreshCurrentUser();
        Users updatedUser = profileService.removeAvatar(currentUser);
        AppSession.setCurrentUser(updatedUser);
        profileNameLabel.setText(buildDisplayName(updatedUser));
        refreshAvatars(updatedUser);
        showInfo("Photo de profil supprimee.");
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        Users currentUser = refreshCurrentUser();
        Users updatedUser = profileService.updatePersonalInfo(
                currentUser,
                prenomField.getText(),
                usernameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                addressField.getText(),
                cityField.getText(),
                codePostalField.getText(),
                countryField.getText()
        );
        AppSession.setCurrentUser(updatedUser);
        profileNameLabel.setText(buildDisplayName(updatedUser));
        profileRoleLabel.setText(roleToLabel(updatedUser.getRole()));
        refreshAvatars(updatedUser);
        showInfo("Informations personnelles mises a jour.");
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        Users currentUser = refreshCurrentUser();
        Users updatedUser = profileService.changePassword(
                currentUser,
                currentPasswordField.getText(),
                newPasswordField.getText(),
                confirmNewPasswordField.getText()
        );
        AppSession.setCurrentUser(updatedUser);
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmNewPasswordField.clear();
        showInfo("Mot de passe mis a jour.");
    }

    private Users refreshCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        Users freshUser = new UsersDao().findById(user.getIdUsers()).orElse(user);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshAvatars(Users user) {
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
        AvatarUtil.applyAvatar(settingsAvatarCircle, settingsAvatarInitialsLabel, user);
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();

        if (prenom.isEmpty() && username.isEmpty()) {
            return "Patient";
        }
        if (prenom.isEmpty()) {
            return username;
        }
        if (username.isEmpty()) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private String roleToLabel(Role role) {
        if (role == null) {
            return "Patient";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Medecin";
            default -> "Patient";
        };
    }
}
