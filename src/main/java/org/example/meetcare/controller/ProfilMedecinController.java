package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.DoctorAccountDao;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.DoctorAccountService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;

public class ProfilMedecinController {
    private final DoctorAccountService doctorAccountService = new DoctorAccountService();

    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitialsLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField specialityField;
    @FXML private TextField cityField;
    @FXML private TextField officeAddressField;
    @FXML private TextArea biographyArea;
    @FXML private TextField sectorField;
    @FXML private TextField consultationPriceField;
    @FXML private CheckBox acceptsVitaleCheckBox;
    @FXML private CheckBox teleconsultationCheckBox;
    @FXML private TextField paymentMethodsField;
    @FXML private TextField spokenLanguagesField;
    @FXML private TextArea officeHoursArea;
    @FXML private TextField experienceField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label profileStatusLabel;
    @FXML private Button saveProfileButton;

    @FXML
    private void initialize() {
        loadProfile();
    }

    @FXML
    private void handleChangePhoto() {
        doctorAccountService.updateDoctorAvatar(saveProfileButton);
        refreshAvatar();
        showInfo("Photo de profil mise a jour.");
    }

    @FXML
    private void handleSavePublicProfile() {
        double price = parseDouble(consultationPriceField.getText());
        int experience = parseInteger(experienceField.getText());

        DoctorAccountDao.DoctorPublicProfileRecord updatedProfile = doctorAccountService.savePublicProfile(
                new DoctorAccountDao.DoctorPublicProfileForm(
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        specialityField.getText().trim(),
                        cityField.getText().trim(),
                        officeAddressField.getText().trim(),
                        biographyArea.getText().trim(),
                        sectorField.getText().trim(),
                        price,
                        acceptsVitaleCheckBox.isSelected(),
                        teleconsultationCheckBox.isSelected(),
                        paymentMethodsField.getText().trim(),
                        spokenLanguagesField.getText().trim(),
                        officeHoursArea.getText().trim(),
                        experience
                )
        );
        fillForm(updatedProfile);
        showInfo("Profil public mis a jour.");
    }

    private void loadProfile() {
        DoctorAccountDao.DoctorPublicProfileRecord profile = doctorAccountService.loadPublicProfile();
        if (profile == null) {
            return;
        }
        fillForm(profile);
        refreshAvatar();
    }

    private void fillForm(DoctorAccountDao.DoctorPublicProfileRecord profile) {
        firstNameField.setText(profile.firstName());
        lastNameField.setText(profile.lastName());
        specialityField.setText(profile.speciality());
        cityField.setText(profile.city());
        officeAddressField.setText(profile.address());
        biographyArea.setText(profile.biography());
        sectorField.setText(profile.sector());
        consultationPriceField.setText(profile.consultationPrice() <= 0 ? "" : String.valueOf((int) profile.consultationPrice()));
        acceptsVitaleCheckBox.setSelected(profile.acceptsVitale());
        teleconsultationCheckBox.setSelected(profile.teleconsultation());
        paymentMethodsField.setText(profile.paymentMethods());
        spokenLanguagesField.setText(profile.spokenLanguages());
        officeHoursArea.setText(profile.officeHours());
        experienceField.setText(profile.experience() <= 0 ? "" : String.valueOf(profile.experience()));
        emailField.setText(profile.email());
        phoneField.setText(profile.phone());
        profileStatusLabel.setText(profile.publicProfileCompleted() ? "Profil public complet" : "Profil public a completer");
    }

    private void refreshAvatar() {
        Users user = AppSession.getCurrentUser();
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
