package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.model.Doctor;
import org.example.meetcare.utils.NavigationUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
    private final DoctorDao doctorDao = new DoctorDao();

    @FXML
    private void initialize() {
        try {
            doctors = loadDoctors();
        } catch (Exception ex) {
            doctors = Collections.emptyList();
        }
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
            emptyState.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

            Label title = new Label("Aucun medecin trouve");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            Label subtitle = new Label("Essaie une autre specialite, ville ou date.");
            subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

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
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        Label avatar = new Label(initialsFromName(doctor.name()));
        avatar.setStyle(
                "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center; -fx-min-width: 60; -fx-min-height: 60; -fx-background-radius: 30;");

        VBox content = new VBox(8);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox();
        VBox identity = new VBox(5);

        Label name = new Label(doctor.name());
        name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label speciality = new Label(doctor.speciality());
        speciality.setStyle("-fx-font-size: 15px; -fx-text-fill: #4b5563;");

        identity.getChildren().addAll(name, speciality);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label profileState = new Label(doctor.publicProfileCompleted() ? "Profil complet" : "Profil a completer");
        profileState.setStyle(doctor.publicProfileCompleted()
                ? "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #16a34a;"
                : "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #b45309;");

        header.getChildren().addAll(identity, spacer, profileState);

        Label address = new Label(doctor.address());
        address.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

        HBox badges = new HBox(10);
        badges.getChildren().addAll(
                createBadge(doctor.sector(), "badge"),
                createBadge(doctor.price(), "badge"));

        if (doctor.experience() != null) {
            badges.getChildren().add(createBadge(doctor.experience() + " ans exp.", "badge"));
        }

        if (doctor.acceptsVitale()) {
            badges.getChildren().add(createBadge("Carte Vitale", "badge-green"));
        } else {
            badges.getChildren().add(createBadge("Sans Vitale", "badge"));
        }

        if (doctor.teleconsultation()) {
            badges.getChildren().add(createBadge("Teleconsultation", "badge-blue"));
        }

        HBox actions = new HBox(15);

        Label nextSlot = new Label(
                "Prochain creneau: " + formatDate(doctor.nextSlotDate()) + " a " + doctor.nextSlotTime());
        nextSlot.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 16px;");

        Region actionsSpacer = new Region();
        HBox.setHgrow(actionsSpacer, Priority.ALWAYS);

        Button profileButton = new Button("Voir profil");
        profileButton.setStyle(
                "-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 15px; -fx-padding: 10 20;");
        profileButton.setOnAction(event -> NavigationUtil.openDoctorPublicProfile(event, doctor.id()));

        Button appointmentButton = new Button("Prendre rendez-vous");
        appointmentButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 10; -fx-padding: 10 20;");
        appointmentButton.setOnAction(event -> handleAppointmentBooking(doctor, appointmentButton));

        actions.getChildren().addAll(nextSlot, actionsSpacer, profileButton, appointmentButton);
        content.getChildren().addAll(header, address, badges, actions);
        card.getChildren().addAll(avatar, content);

        return card;
    }

    private void handleAppointmentBooking(DoctorViewData doctor, Button sourceButton) {
        openBookingPage(sourceButton, doctor);
    }

    private void openBookingPage(Button sourceButton, DoctorViewData doctor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/appointmentBookingPatient.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            AppointmentBookingPatientController controller = loader.getController();
            controller.setInitialDoctor(doctor.id(), doctor.name(), doctor.speciality(), doctor.address(),
                    doctor.nextSlotDate(), doctor.nextSlotTime());
            controller.applyInitialDoctorSelection();

            Stage stage = (Stage) sourceButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException exception) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Erreur");
            errorAlert.setHeaderText("Impossible d'ouvrir la page de prise de rendez-vous");
            errorAlert.setContentText("Verifie que appointmentBookingPatient.fxml est bien disponible.");
            errorAlert.showAndWait();
        }
    }

    private Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.setStyle(badgeStyle(styleClass));
        return badge;
    }

    private String badgeStyle(String styleClass) {
        if ("badge-green".equals(styleClass)) {
            return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-border-color: #86efac; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5 12;";
        }
        if ("badge-blue".equals(styleClass)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5 12;";
        }
        return "-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5 12;";
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private String formatDate(LocalDate date) {
        return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear();
    }

    private String initialsFromName(String name) {
        if (name == null || name.isBlank()) {
            return "D";
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 4) {
            return trimmed.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return trimmed.substring(4, Math.min(5, trimmed.length())).toUpperCase(Locale.ROOT);
    }

    private List<DoctorViewData> loadDoctors() {
        List<Doctor> doctorRows = doctorDao.searchDoctors("", "");
        List<DoctorViewData> viewData = new ArrayList<>();
        int index = 0;
        for (Doctor doctor : doctorRows) {
            viewData.add(toViewData(doctor, index++));
        }
        return viewData;
    }

    private DoctorViewData toViewData(Doctor doctor, int index) {
        LocalDate nextDate = LocalDate.now().plusDays((doctor.getId() % 5) + 1L);
        String nextTime = switch (index % 14) {
            case 0 -> "09:00";
            case 1 -> "09:30";
            case 2 -> "10:00";
            case 3 -> "10:30";
            case 4 -> "11:00";
            case 5 -> "11:30";
            case 6 -> "12:00";
            case 7 -> "12:30";
            case 8 -> "13:00";
            case 9 -> "13:30";
            case 10 -> "14:00";
            case 11 -> "14:30";
            case 12 -> "15:00";
            default -> "15:30";
        };

        return new DoctorViewData(
                doctor.getId(),
                "Dr. " + doctor.getUsername(),
                doctor.getSpeciality(),
                doctor.getCity(),
                doctor.getAddress(),
                doctor.getSector(),
                doctor.getPrice() + " EUR",
                doctor.isAcceptsVitale(),
                doctor.isTeleconsultation(),
                doctor.getExperience(),
                doctor.getBiography(),
                doctor.isPublicProfileCompleted(),
                nextDate,
                nextTime);
    }

    private record DoctorViewData(
            int id,
            String name,
            String speciality,
            String city,
            String address,
            String sector,
            String price,
            boolean acceptsVitale,
            boolean teleconsultation,
            Integer experience,
            String biography,
            boolean publicProfileCompleted,
            LocalDate nextSlotDate,
            String nextSlotTime) {
    }
}
