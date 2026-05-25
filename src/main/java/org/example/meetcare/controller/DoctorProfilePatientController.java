package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.example.meetcare.dao.PatientCareDao;
import org.example.meetcare.model.AvailabilitySlot;
import org.example.meetcare.model.DoctorProfileData;
import org.example.meetcare.utils.DoctorProfileStore;
import org.example.meetcare.utils.NavigationUtil;

public class DoctorProfilePatientController {
    @FXML private Label doctorAvatarLabel;
    @FXML private Label doctorNameLabel;
    @FXML private Label specialityLabel;
    @FXML private Label cityLabel;
    @FXML private Label ratingLabel;
    @FXML private Label experienceLabel;
    @FXML private Label priceLabel;
    @FXML private Label sectorLabel;
    @FXML private Label addressLabel;
    @FXML private Label emailLabel;
    @FXML private Label vitaleLabel;
    @FXML private Label teleconsultationLabel;
    @FXML private Label nextSlotLabel;
    @FXML private Label biographyLabel;
    @FXML private ComboBox<AvailabilitySlot> slotComboBox;
    @FXML private Button appointmentButton;

    private final PatientCareDao patientCareDao = new PatientCareDao();
    private DoctorProfileData currentDoctor;

    @FXML
    private void initialize() {
        currentDoctor = DoctorProfileStore.getCurrentDoctor();
        if (currentDoctor == null) {
            doctorNameLabel.setText("Aucun medecin selectionne");
            specialityLabel.setText("Retourne a la recherche pour ouvrir une fiche.");
            appointmentButton.setDisable(true);
            return;
        }

        doctorAvatarLabel.setText(currentDoctor.getAvatarInitial());
        doctorNameLabel.setText(currentDoctor.getName());
        specialityLabel.setText(currentDoctor.getSpeciality());
        cityLabel.setText(currentDoctor.getCity());
        ratingLabel.setText("Note " + currentDoctor.getRating());
        experienceLabel.setText(currentDoctor.getExperience() + " ans d'experience");
        priceLabel.setText(currentDoctor.getPrice());
        sectorLabel.setText(currentDoctor.getSector());
        addressLabel.setText(currentDoctor.getAddress());
        emailLabel.setText(currentDoctor.getEmail());
        vitaleLabel.setText(currentDoctor.isAcceptsVitale() ? "Carte Vitale acceptee" : "Carte Vitale non acceptee");
        teleconsultationLabel.setText(currentDoctor.isTeleconsultation() ? "Teleconsultation disponible" : "Consultation au cabinet");
        biographyLabel.setText(currentDoctor.getBiography());

        slotComboBox.getItems().addAll(currentDoctor.getAvailableSlots());
        slotComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(AvailabilitySlot item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toDisplayLabel());
            }
        });
        slotComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(AvailabilitySlot item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisir un creneau" : item.toDisplayLabel());
            }
        });

        if (currentDoctor.getAvailableSlots().isEmpty()) {
            nextSlotLabel.setText("Aucun creneau disponible pour le moment");
            appointmentButton.setDisable(true);
        } else {
            slotComboBox.setValue(currentDoctor.getAvailableSlots().get(0));
            nextSlotLabel.setText(currentDoctor.getAvailableSlots().get(0).toDisplayLabel());
            slotComboBox.setOnAction(event -> {
                AvailabilitySlot selectedSlot = slotComboBox.getValue();
                nextSlotLabel.setText(selectedSlot == null ? "Choisir un creneau" : selectedSlot.toDisplayLabel());
            });
        }
    }

    @FXML
    private void handleBackToSearch() {
        NavigationUtil.navigateTo(appointmentButton, "/view/searchDoctor.fxml", 1200, 800, "MeetCare - Recherche");
    }

    @FXML
    private void handleBookAppointment() {
        if (currentDoctor == null) {
            return;
        }

        AvailabilitySlot selectedSlot = slotComboBox.getValue();
        if (selectedSlot == null) {
            showInfo("Creneau manquant", "Choisis d'abord un creneau disponible.");
            return;
        }

        boolean created = patientCareDao.createAppointment(currentDoctor.getDoctorId(), selectedSlot, currentDoctor.isTeleconsultation());
        if (created) {
            NavigationUtil.navigateTo(appointmentButton, "/view/myAppointments.fxml", 1200, 720, "MeetCare - Mes rendez-vous");
        } else {
            showInfo("Creneau indisponible", "Ce creneau vient d'etre reserve ou n'est plus disponible.");
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
