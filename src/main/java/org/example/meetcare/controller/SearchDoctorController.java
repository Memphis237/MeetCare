package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.utils.AppointmentStore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class SearchDoctorController {

    @FXML
    private TextField searchField;

    @FXML
    private TextField cityField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private VBox doctorResultsContainer;

    @FXML
    private Label resultsCountLabel;

    private List<DoctorViewData> doctors;

    @FXML
    private void initialize() {
        doctors = List.of(
                new DoctorViewData(
                        "Dr. Sarah Martin",
                        "Cardiologue",
                        "Paris",
                        "15 Avenue des Champs-Elysees, 75008 Paris",
                        "Secteur 1",
                        "30 EUR",
                        true,
                        true,
                        "4.8",
                        LocalDate.of(2026, 5, 11),
                        "14:30"
                ),
                new DoctorViewData(
                        "Dr. Marc Dubois",
                        "Generaliste",
                        "Lyon",
                        "23 Rue de la Republique, 69002 Lyon",
                        "Secteur 1",
                        "25 EUR",
                        true,
                        false,
                        "4.9",
                        LocalDate.of(2026, 5, 12),
                        "10:00"
                ),
                new DoctorViewData(
                        "Dr. Julie Bernard",
                        "Dermatologue",
                        "Paris",
                        "8 Boulevard Saint-Germain, 75005 Paris",
                        "Secteur 2",
                        "50 EUR",
                        true,
                        true,
                        "4.7",
                        LocalDate.of(2026, 5, 13),
                        "16:00"
                ),
                new DoctorViewData(
                        "Dr. Amine Rahmani",
                        "Pediatre",
                        "Marseille",
                        "42 Rue Paradis, 13001 Marseille",
                        "Secteur 1",
                        "35 EUR",
                        true,
                        false,
                        "4.6",
                        LocalDate.of(2026, 5, 14),
                        "09:15"
                ),
                new DoctorViewData(
                        "Dr. Lea Moreau",
                        "Ophtalmologue",
                        "Toulouse",
                        "12 Allees Jean Jaures, 31000 Toulouse",
                        "Secteur 2",
                        "60 EUR",
                        true,
                        true,
                        "4.9",
                        LocalDate.of(2026, 5, 15),
                        "11:45"
                )
        );

        refreshResults(doctors);
    }

    @FXML
    private void handleSearch() {
        String keyword = normalize(searchField.getText());
        String city = normalize(cityField.getText());
        LocalDate selectedDate = datePicker.getValue();

        List<DoctorViewData> filteredDoctors = doctors.stream()
                .filter(doctor -> keyword.isBlank()
                        || normalize(doctor.name()).contains(keyword)
                        || normalize(doctor.speciality()).contains(keyword)
                        || normalize(doctor.address()).contains(keyword))
                .filter(doctor -> city.isBlank() || normalize(doctor.city()).contains(city))
                .filter(doctor -> selectedDate == null || !doctor.nextSlotDate().isBefore(selectedDate))
                .collect(Collectors.toList());

        refreshResults(filteredDoctors);
    }

    private void refreshResults(List<DoctorViewData> doctorsToDisplay) {
        doctorResultsContainer.getChildren().clear();
        resultsCountLabel.setText(doctorsToDisplay.size() + (doctorsToDisplay.size() > 1 ? " resultats" : " resultat"));

        if (doctorsToDisplay.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.getStyleClass().add("doctor-card");

            Label title = new Label("Aucun medecin trouve");
            title.getStyleClass().add("doctor-name");

            Label subtitle = new Label("Essaie une autre specialite, ville ou date.");
            subtitle.getStyleClass().add("doctor-info");

            emptyState.getChildren().addAll(title, subtitle);
            doctorResultsContainer.getChildren().add(emptyState);
            return;
        }

        for (DoctorViewData doctor : doctorsToDisplay) {
            doctorResultsContainer.getChildren().add(createDoctorCard(doctor));
        }
    }

    private HBox createDoctorCard(DoctorViewData doctor) {
        HBox card = new HBox(25);
        card.getStyleClass().add("doctor-card");

        Label avatar = new Label(doctor.name().substring(4, 5).toUpperCase(Locale.ROOT));
        avatar.getStyleClass().add("avatar");

        VBox content = new VBox(8);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox();
        VBox identity = new VBox(5);

        Label name = new Label(doctor.name());
        name.getStyleClass().add("doctor-name");

        Label speciality = new Label(doctor.speciality());
        speciality.getStyleClass().add("doctor-speciality");

        identity.getChildren().addAll(name, speciality);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rating = new Label("Note " + doctor.rating());
        rating.getStyleClass().add("rating");

        header.getChildren().addAll(identity, spacer, rating);

        Label address = new Label(doctor.address());
        address.getStyleClass().add("doctor-info");

        HBox badges = new HBox(10);
        badges.getChildren().addAll(
                createBadge(doctor.sector(), "badge"),
                createBadge(doctor.price(), "badge"),
                createBadge("Carte Vitale", "badge-green")
        );

        if (doctor.teleconsultation()) {
            badges.getChildren().add(createBadge("Teleconsultation", "badge-blue"));
        }

        HBox actions = new HBox(15);

        Label nextSlot = new Label("Prochain creneau: " + formatDate(doctor.nextSlotDate()) + " a " + doctor.nextSlotTime());
        nextSlot.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 16px;");

        Region actionsSpacer = new Region();
        HBox.setHgrow(actionsSpacer, Priority.ALWAYS);

        Button profileButton = new Button("Voir profil");
        profileButton.getStyleClass().add("secondary-button");

        Button appointmentButton = new Button("Prendre rendez-vous");
        appointmentButton.getStyleClass().add("primary-button");
        appointmentButton.setOnAction(event -> handleAppointmentBooking(doctor, appointmentButton));

        actions.getChildren().addAll(nextSlot, actionsSpacer, profileButton, appointmentButton);
        content.getChildren().addAll(header, address, badges, actions);
        card.getChildren().addAll(avatar, content);

        return card;
    }

    private void handleAppointmentBooking(DoctorViewData doctor, Button sourceButton) {
        ButtonType confirmButton = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation du rendez-vous");
        alert.setHeaderText("Prendre rendez-vous avec " + doctor.name());
        alert.setContentText("Confirmer le creneau du " + formatDate(doctor.nextSlotDate()) + " a " + doctor.nextSlotTime() + " ?");
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            AppointmentStore.addAppointment(AppointmentStore.createAppointment(
                    doctor.name(),
                    doctor.speciality(),
                    doctor.city(),
                    doctor.address(),
                    AppointmentStore.STATUS_CONFIRMED,
                    doctor.teleconsultation() ? "Motif: Teleconsultation reservee" : "Motif: Consultation reservee",
                    doctor.teleconsultation(),
                    doctor.nextSlotDate(),
                    doctor.nextSlotTime()
            ));
            openMyAppointmentsPage(sourceButton);
        }
    }

    private void openMyAppointmentsPage(Button sourceButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/myAppointments.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = (Stage) sourceButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException exception) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Erreur");
            errorAlert.setHeaderText("Impossible d'ouvrir la page des rendez-vous");
            errorAlert.setContentText("Verifie que myAppointments.fxml est bien disponible.");
            errorAlert.showAndWait();
        }
    }

    private Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().add(styleClass);
        return badge;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private String formatDate(LocalDate date) {
        return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear();
    }

    private record DoctorViewData(
            String name,
            String speciality,
            String city,
            String address,
            String sector,
            String price,
            boolean acceptsVitale,
            boolean teleconsultation,
            String rating,
            LocalDate nextSlotDate,
            String nextSlotTime
    ) {
    }
}
