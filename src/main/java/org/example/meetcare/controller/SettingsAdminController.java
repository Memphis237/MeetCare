package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.DoctorProfileService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;

import java.util.Optional;

public class SettingsAdminController {
    private final DoctorProfileService profileService = new DoctorProfileService();

    @FXML
    private SidebarAdminController sidebarAdminController;

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
        try {
            Users currentUser = refreshCurrentUser();
            Users updatedUser = profileService.updateAvatar(currentUser, (Node) event.getSource());
            AppSession.setCurrentUser(updatedUser);
            profileNameLabel.setText(buildDisplayName(updatedUser));
            refreshAvatars(updatedUser);
            refreshSidebar(updatedUser);
            showInfo("Photo de profil mise à jour.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleDeletePhoto(ActionEvent event) {
        try {
            Users currentUser = refreshCurrentUser();
            Users updatedUser = profileService.removeAvatar(currentUser);
            AppSession.setCurrentUser(updatedUser);
            profileNameLabel.setText(buildDisplayName(updatedUser));
            refreshAvatars(updatedUser);
            refreshSidebar(updatedUser);
            showInfo("Photo de profil supprimée.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        try {
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
            refreshSidebar(updatedUser);
            showInfo("Informations personnelles mises à jour.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        try {
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
            showInfo("Mot de passe mis à jour.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteAccount(ActionEvent event) {
        Users currentUser = refreshCurrentUser();
        if (currentUser == null) {
            showError("Aucun utilisateur connecté.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Suppression du compte");
        confirmation.setHeaderText("Voulez-vous vraiment supprimer votre compte ?");
        confirmation.setContentText("Cette action est définitive. Vos données liées seront également supprimées.");

        ButtonType deleteButton = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(deleteButton, cancelButton);

        if (event.getSource() instanceof Node node && node.getScene() != null && node.getScene().getWindow() != null) {
            confirmation.initOwner(node.getScene().getWindow());
        }

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != deleteButton) {
            return;
        }

        try {
            profileService.deleteAccount(currentUser);
            AppSession.clear();
            navigateToLogin((Node) event.getSource());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
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

    private void refreshAvatars(Users user) {
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
        AvatarUtil.applyAvatar(settingsAvatarCircle, settingsAvatarInitialsLabel, user);
    }

    private void refreshSidebar(Users user) {
        if (sidebarAdminController != null) {
            sidebarAdminController.refreshSidebar(user);
        }
    }

    private void navigateToLogin(Node source) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 620, 520);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("MeetCare - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception ex) {
            showError("Impossible de revenir à la connexion.");
        }
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();

        if (prenom.isEmpty() && username.isEmpty()) {
            return "Admin";
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
            return "Admin";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Médecin";
            default -> "Patient";
        };
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank() ? "Une erreur est survenue." : message);
        alert.showAndWait();
    }
}
