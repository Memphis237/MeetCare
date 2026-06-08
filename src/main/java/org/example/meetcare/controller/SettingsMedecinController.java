package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.DoctorProfileService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.NavigationUtil;

import java.util.Optional;

public class SettingsMedecinController {
    private final DoctorProfileService profileService = new DoctorProfileService();
    private final DoctorDao doctorDao = new DoctorDao();

    @FXML
    private SidebarMedecinController sidebarMedecinController;

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
    private TextField specialityField;

    @FXML
    private TextField sectorField;

    @FXML
    private TextField experienceField;

    @FXML
    private TextField consultationPriceField;

    @FXML
    private TextArea biographyArea;

    @FXML
    private TextField publicAddressField;

    @FXML
    private CheckBox acceptsVitaleToggle;

    @FXML
    private CheckBox teleconsultationToggle;

    @FXML
    private Label publicProfileStatusLabel;

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
        refreshPublicProfileFields(user);
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
            showInfo("Photo de profil mise a jour.");
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
            showInfo("Photo de profil supprimee.");
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
            showInfo("Informations personnelles mises a jour.");
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
            showInfo("Mot de passe mis a jour.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleSavePublicProfile(ActionEvent event) {
        Users currentUser = refreshCurrentUser();
        if (currentUser == null) {
            showError("Aucun utilisateur connecte.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer l'enregistrement");
        confirmation.setHeaderText("Enregistrer le profil public ?");
        confirmation.setContentText("Les informations du profil public seront sauvegardees en base de donnees et visibles sur l'application.");

        ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(saveButton, cancelButton);

        if (event.getSource() instanceof Node node && node.getScene() != null && node.getScene().getWindow() != null) {
            confirmation.initOwner(node.getScene().getWindow());
        }

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != saveButton) {
            return;
        }

        try {
            profileService.updatePublicProfile(
                    currentUser,
                    specialityField == null ? null : specialityField.getText(),
                    publicAddressField == null ? null : publicAddressField.getText(),
                    sectorField == null ? null : sectorField.getText(),
                    consultationPriceField == null ? null : consultationPriceField.getText(),
                    experienceField == null ? null : experienceField.getText(),
                    biographyArea == null ? null : biographyArea.getText(),
                    acceptsVitaleToggle != null && acceptsVitaleToggle.isSelected(),
                    teleconsultationToggle != null && teleconsultationToggle.isSelected()
            );
            refreshPublicProfileFields(currentUser);
            showInfo("Profil public mis a jour.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handlePreviewPublicProfile(ActionEvent event) {
        try {
            Users currentUser = refreshCurrentUser();
            if (currentUser == null) {
                showError("Aucun utilisateur connecte.");
                return;
            }

            int doctorId = doctorDao.ensureDoctorIdForUser(currentUser.getIdUsers());
            NavigationUtil.openDoctorPublicProfile(event, doctorId);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteAccount(ActionEvent event) {
        Users currentUser = refreshCurrentUser();
        if (currentUser == null) {
            showError("Aucun utilisateur connecte.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Suppression du compte");
        confirmation.setHeaderText("Voulez-vous vraiment supprimer votre compte ?");
        confirmation.setContentText("Cette action est definitive. Vos donnees liees seront egalement supprimees.");

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

    private void refreshPublicProfileFields(Users user) {
        try {
            refreshPublicProfileFields(profileService.loadPublicProfile(user));
        } catch (Exception ex) {
            if (publicProfileStatusLabel != null) {
                publicProfileStatusLabel.setText("Profil public a configurer");
            }
        }
    }

    private void refreshPublicProfileFields(org.example.meetcare.model.DoctorAdminEntry profile) {
        if (profile == null) {
            return;
        }

        if (specialityField != null) {
            specialityField.setText(safeText(profile.getSpeciality()));
        }
        if (sectorField != null) {
            sectorField.setText(safeText(profile.getSector()));
        }
        if (experienceField != null) {
            experienceField.setText(profile.getExperience() == null ? "" : String.valueOf(profile.getExperience()));
        }
        if (consultationPriceField != null) {
            consultationPriceField.setText(profile.getConsultationPrice() == null ? "" : String.format(java.util.Locale.FRANCE, "%.2f", profile.getConsultationPrice()));
        }
        if (biographyArea != null) {
            biographyArea.setText(safeText(profile.getBiography()));
        }
        if (publicAddressField != null) {
            publicAddressField.setText(safeText(profile.getDoctorAddress()));
        }
        if (acceptsVitaleToggle != null) {
            acceptsVitaleToggle.setSelected(profile.isAcceptsVitale());
        }
        if (teleconsultationToggle != null) {
            teleconsultationToggle.setSelected(profile.isTeleconsultation());
        }
        if (publicProfileStatusLabel != null) {
            publicProfileStatusLabel.setText(profile.isPublicProfileCompleted()
                    ? "Profil public actif"
                    : "Profil public a finaliser");
        }
    }

    private void refreshSidebar(Users user) {
        if (sidebarMedecinController != null) {
            sidebarMedecinController.refreshSidebar(user);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
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
            throw new IllegalStateException("Impossible d'ouvrir la page de connexion.", ex);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("MeetCare");
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank()
                ? "Une erreur est survenue."
                : message);
        alert.showAndWait();
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();

        if (prenom.isEmpty() && username.isEmpty()) {
            return "Medecin";
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
            return "Medecin";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Medecin";
            default -> "Patient";
        };
    }
}
