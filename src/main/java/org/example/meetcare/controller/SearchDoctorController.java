package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.PatientCareDao;
import org.example.meetcare.model.AvailabilitySlot;
import org.example.meetcare.model.Doctor;
import org.example.meetcare.model.DoctorProfileData;
import org.example.meetcare.utils.DoctorProfileStore;
import org.example.meetcare.utils.NavigationUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SearchDoctorController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private TextField searchField;
    @FXML private TextField cityField;
    @FXML private DatePicker datePicker;
    @FXML private VBox doctorResultsContainer;
    @FXML private Label resultsCountLabel;

    private final PatientCareDao patientCareDao = new PatientCareDao();

    @FXML
    private void initialize() {
        loadDoctors();
    }

    @FXML
    private void handleSearch() {
        loadDoctors();
    }

    private void loadDoctors() {
        LocalDate selectedDate = datePicker == null ? null : datePicker.getValue();
        List<PatientCareDao.DoctorSearchRecord> doctors = patientCareDao.searchDoctors(
                safe(searchField.getText()),
                safe(cityField.getText()),
                selectedDate
        );
        refreshResults(doctors);
    }

    private void refreshResults(List<PatientCareDao.DoctorSearchRecord> doctorsToDisplay) {
        doctorResultsContainer.getChildren().clear();
        resultsCountLabel.setText(doctorsToDisplay.size() + (doctorsToDisplay.size() > 1 ? " resultats" : " resultat"));

        if (doctorsToDisplay.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.getStyleClass().add("doctor-card");

            Label title = new Label("Aucun medecin trouve");
            title.getStyleClass().add("doctor-name");

            Label subtitle = new Label("Essaie une autre ville, une autre specialite ou demande d'abord au medecin d'ouvrir des disponibilites.");
            subtitle.getStyleClass().add("doctor-info");

            emptyState.getChildren().addAll(title, subtitle);
            doctorResultsContainer.getChildren().add(emptyState);
            return;
        }

        for (PatientCareDao.DoctorSearchRecord doctor : doctorsToDisplay) {
            doctorResultsContainer.getChildren().add(createDoctorCard(doctor));
        }
    }

    private HBox createDoctorCard(PatientCareDao.DoctorSearchRecord doctorRecord) {
        Doctor doctor = doctorRecord.doctor();
        List<AvailabilitySlot> slots = doctorRecord.availableSlots();

        HBox card = new HBox(25);
        card.getStyleClass().add("doctor-card");

        Label avatar = new Label(getAvatarInitial(doctor.getUsername()));
        avatar.getStyleClass().add("avatar");

        VBox content = new VBox(8);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox();
        VBox identity = new VBox(5);

        Label name = new Label(formatDoctorName(doctor.getUsername()));
        name.getStyleClass().add("doctor-name");

        Label speciality = new Label(safeValue(doctor.getSpeciality(), "Specialite non renseignee"));
        speciality.getStyleClass().add("doctor-speciality");
        identity.getChildren().addAll(name, speciality);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rating = new Label("Experience " + Math.max(doctor.getExperience(), 0) + " ans");
        rating.getStyleClass().add("rating");

        header.getChildren().addAll(identity, spacer, rating);

        Label address = new Label(buildAddress(doctor.getAddress(), doctor.getCity()));
        address.getStyleClass().add("doctor-info");

        HBox badges = new HBox(10);
        badges.getChildren().addAll(
                createBadge(safeValue(doctor.getSector(), "Secteur non renseigne"), "badge"),
                createBadge(formatPrice(doctor.getPrice()), "badge")
        );
        if (doctor.isAcceptsVitale()) {
            badges.getChildren().add(createBadge("Carte Vitale", "badge-green"));
        }
        if (doctor.isTeleconsultation()) {
            badges.getChildren().add(createBadge("Teleconsultation", "badge-blue"));
        }

        VBox availabilityBox = new VBox(8);
        Label availabilityTitle = new Label("Creneaux disponibles");
        availabilityTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        ComboBox<AvailabilitySlot> slotComboBox = new ComboBox<>();
        slotComboBox.setPrefWidth(280);
        slotComboBox.setPromptText("Choisir un creneau");
        slotComboBox.getItems().addAll(slots);
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
        if (!slots.isEmpty()) {
            slotComboBox.setValue(slots.get(0));
        }

        Label noSlotLabel = new Label("Aucun creneau disponible pour le moment");
        noSlotLabel.getStyleClass().add("doctor-info");
        noSlotLabel.setVisible(slots.isEmpty());
        noSlotLabel.setManaged(slots.isEmpty());
        slotComboBox.setVisible(!slots.isEmpty());
        slotComboBox.setManaged(!slots.isEmpty());

        availabilityBox.getChildren().addAll(availabilityTitle, slotComboBox, noSlotLabel);

        HBox actions = new HBox(15);
        Region actionsSpacer = new Region();
        HBox.setHgrow(actionsSpacer, Priority.ALWAYS);

        Button profileButton = new Button("Voir profil");
        profileButton.getStyleClass().add("secondary-button");
        profileButton.setOnAction(event -> handleViewProfile(doctor, slots, profileButton));

        Button appointmentButton = new Button("Prendre rendez-vous");
        appointmentButton.getStyleClass().add("primary-button");
        appointmentButton.setDisable(slots.isEmpty());
        appointmentButton.setOnAction(event -> handleAppointmentBooking(doctor, slotComboBox.getValue(), appointmentButton));

        actions.getChildren().addAll(actionsSpacer, profileButton, appointmentButton);

        content.getChildren().addAll(header, address, badges, availabilityBox, actions);
        card.getChildren().addAll(avatar, content);
        return card;
    }

    private void handleViewProfile(Doctor doctor, List<AvailabilitySlot> slots, Button sourceButton) {
        DoctorProfileStore.setCurrentDoctor(new DoctorProfileData(
                doctor.getId(),
                formatDoctorName(doctor.getUsername()),
                safeValue(doctor.getEmail(), "Email non renseigne"),
                safeValue(doctor.getSpeciality(), "Specialite non renseignee"),
                safeValue(doctor.getCity(), "Ville non renseignee"),
                buildAddress(doctor.getAddress(), doctor.getCity()),
                safeValue(doctor.getSector(), "Secteur non renseigne"),
                formatPrice(doctor.getPrice()),
                doctor.isAcceptsVitale(),
                doctor.isTeleconsultation(),
                buildRating(doctor.getExperience()),
                Math.max(doctor.getExperience(), 0),
                safeValue(doctor.getBiography(), "Ce medecin n'a pas encore ajoute de presentation."),
                slots
        ));
        NavigationUtil.navigateTo(sourceButton, "/view/doctorProfilePatient.fxml", 1200, 800, "MeetCare - Profil medecin");
    }

    private void handleAppointmentBooking(Doctor doctor, AvailabilitySlot slot, Button sourceButton) {
        if (slot == null) {
            showInfo("Creneau manquant", "Choisis d'abord un creneau disponible.");
            return;
        }

        ButtonType confirmButton = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation du rendez-vous");
        alert.setHeaderText("Prendre rendez-vous avec " + formatDoctorName(doctor.getUsername()));
        alert.setContentText("Confirmer le creneau du " + slot.toDisplayLabel() + " ?");
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            boolean created = patientCareDao.createAppointment(doctor.getId(), slot, doctor.isTeleconsultation());
            if (created) {
                NavigationUtil.navigateTo(sourceButton, "/view/myAppointments.fxml", 1200, 720, "MeetCare - Mes rendez-vous");
            } else {
                showInfo("Creneau indisponible", "Ce creneau vient d'etre reserve ou n'est plus disponible.");
                loadDoctors();
            }
        }
    }

    private Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().add(styleClass);
        return badge;
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatDoctorName(String username) {
        if (username == null || username.isBlank()) {
            return "Dr. Inconnu";
        }
        return username.startsWith("Dr.") ? username : "Dr. " + username;
    }

    private String buildAddress(String address, String city) {
        if (address == null || address.isBlank()) {
            return safeValue(city, "Adresse non renseignee");
        }
        if (city == null || city.isBlank() || address.toLowerCase(Locale.ROOT).contains(city.toLowerCase(Locale.ROOT))) {
            return address;
        }
        return address + ", " + city;
    }

    private String formatPrice(double price) {
        return String.format(Locale.US, "%.0f EUR", price);
    }

    private String buildRating(int experience) {
        double rating = Math.max(4.2, Math.min(5.0, 4.3 + (experience * 0.03)));
        return String.format(Locale.US, "%.1f", rating);
    }

    private String getAvatarInitial(String doctorName) {
        String cleanName = doctorName == null ? "" : doctorName.replace("Dr. ", "").trim();
        if (cleanName.isEmpty()) {
            return "D";
        }
        return cleanName.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
