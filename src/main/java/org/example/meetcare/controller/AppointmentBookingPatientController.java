package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.model.Doctor;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.NavigationUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentBookingPatientController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final List<DoctorData> doctors = new ArrayList<>();

    private Integer initialDoctorId;
    private String initialDoctorName;
    private String initialDoctorSpeciality;
    private String initialDoctorAddress;
    private LocalDate initialDate;
    private String initialTime;

    @FXML
    private ComboBox<String> doctorComboBox;

    @FXML
    private DatePicker appointmentDatePicker;

    @FXML
    private ComboBox<String> timeComboBox;

    @FXML
    private TextField motifField;

    @FXML
    private Circle doctorAvatarCircle;

    @FXML
    private Label doctorNameLabel;

    @FXML
    private Label doctorSpecialityLabel;

    @FXML
    private Label doctorAddressLabel;

    @FXML
    private Label availabilityHintLabel;

    @FXML
    private Label summaryDoctorLabel;

    @FXML
    private Label summaryDateLabel;

    @FXML
    private Label summaryTimeLabel;

    @FXML
    private Label summaryMotifLabel;

    @FXML
    private void initialize() {
        loadDoctorsFromDatabase();

        doctorComboBox.setItems(FXCollections.observableArrayList(
                doctors.stream().map(DoctorData::displayLabel).toList()
        ));
        doctorComboBox.setValue(resolveInitialDoctor().displayLabel());

        timeComboBox.setItems(FXCollections.observableArrayList(buildTimes()));
        timeComboBox.setValue(initialTime == null ? "09:00" : initialTime);

        appointmentDatePicker.setValue(initialDate == null ? LocalDate.now().plusDays(1) : initialDate);
        appointmentDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });

        doctorComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateDoctorDetails());
        appointmentDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> updateSummary());
        timeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateSummary());
        motifField.textProperty().addListener((obs, oldValue, newValue) -> updateSummary());

        updateDoctorDetails();
        updateSummary();
        updateAvailabilityHint();
    }

    public void setInitialDoctor(int doctorId, String name, String speciality, String address, LocalDate date, String time) {
        this.initialDoctorId = doctorId;
        this.initialDoctorName = name;
        this.initialDoctorSpeciality = speciality;
        this.initialDoctorAddress = address;
        this.initialDate = date;
        this.initialTime = time;
    }

    public void applyInitialDoctorSelection() {
        if (doctorComboBox == null) {
            return;
        }

        doctorComboBox.setValue(resolveInitialDoctor().displayLabel());
        if (initialDate != null) {
            appointmentDatePicker.setValue(initialDate);
        }
        if (initialTime != null) {
            timeComboBox.setValue(initialTime);
        }
        updateDoctorDetails();
        updateAvailabilityHint();
        updateSummary();
    }

    @FXML
    private void handleDoctorSelection(ActionEvent event) {
        updateDoctorDetails();
    }

    @FXML
    private void handleDateChange(ActionEvent event) {
        updateSummary();
        updateAvailabilityHint();
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        NavigationUtil.openPatientAppointments(event);
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        DoctorData selectedDoctor = selectedDoctor();
        LocalDate selectedDate = appointmentDatePicker.getValue();
        String selectedTime = timeComboBox.getValue();
        String motif = motifField.getText() == null ? "" : motifField.getText().trim();

        if (selectedDoctor == null || selectedDate == null || selectedTime == null || motif.isBlank()) {
            showCustomDialog(getWindow(event), "Champs manquants",
                    "Merci de remplir le médecin, la date, l'heure et le motif avant de confirmer.");
            return;
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            showCustomDialog(getWindow(event), "Date invalide", "Choisis une date à partir d'aujourd'hui.");
            return;
        }

        if (!appointmentDao.isSlotAvailable(selectedDoctor.id(), selectedDate, selectedTime)) {
            showSlotUnavailableDialog(getWindow(event), selectedDoctor, selectedDate, selectedTime);
            return;
        }

        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            showCustomDialog(getWindow(event), "Session invalide", "Aucun patient connecte.");
            return;
        }

        appointmentDao.insertAppointment(
                selectedDoctor.id(),
                currentUser.getIdUsers(),
                selectedDate,
                selectedTime,
                "Motif: " + motif
        );

        showSuccessDialog(getWindow(event), selectedDoctor, selectedDate, selectedTime, event);
    }

    private void loadDoctorsFromDatabase() {
        doctors.clear();
        for (Doctor doctor : doctorDao.searchDoctors("", "")) {
            doctors.add(new DoctorData(
                    doctor.getId(),
                    "Dr. " + doctor.getUsername(),
                    doctor.getSpeciality(),
                    doctor.getAddress(),
                    doctor.getCity()
            ));
        }

        if (doctors.isEmpty()) {
            doctors.add(new DoctorData(0, "Aucun médecin", "Aucune spécialité", "", ""));
        }
    }

    private void updateDoctorDetails() {
        DoctorData doctor = selectedDoctor();
        if (doctor == null) {
            return;
        }

        doctorNameLabel.setText(doctor.name());
        doctorSpecialityLabel.setText(doctor.speciality());
        doctorAddressLabel.setText(doctor.address());
        availabilityHintLabel.setText("Les créneaux sont ouverts de 09:00 à 16:00.");
        updateAvailabilityHint();
        updateSummary();
    }

    private void updateSummary() {
        DoctorData doctor = selectedDoctor();
        summaryDoctorLabel.setText("Médecin: " + (doctor == null ? "-" : doctor.displayLabel()));
        summaryDateLabel.setText("Date: " + (appointmentDatePicker.getValue() == null ? "-" : appointmentDatePicker.getValue().format(DATE_FORMATTER)));
        summaryTimeLabel.setText("Heure: " + (timeComboBox.getValue() == null ? "-" : timeComboBox.getValue()));
        summaryMotifLabel.setText("Motif: " + (motifField.getText() == null || motifField.getText().isBlank() ? "-" : motifField.getText().trim()));
    }

    private void updateAvailabilityHint() {
        DoctorData doctor = selectedDoctor();
        if (doctor == null || appointmentDatePicker.getValue() == null || timeComboBox.getValue() == null) {
            availabilityHintLabel.setText("Choisis une date et une heure pour vérifier la disponibilité.");
            return;
        }

        boolean available = appointmentDao.isSlotAvailable(doctor.id(), appointmentDatePicker.getValue(), timeComboBox.getValue());
        availabilityHintLabel.setText(available
                ? "Créneau disponible pour cette date."
                : "Ce créneau est déjà réservé. Choisis un autre horaire.");
        availabilityHintLabel.setStyle(available
                ? "-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-background-radius: 12; -fx-padding: 10 12; -fx-font-size: 12px;"
                : "-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c; -fx-background-radius: 12; -fx-padding: 10 12; -fx-font-size: 12px;");
    }

    private DoctorData selectedDoctor() {
        String value = doctorComboBox.getValue();
        if (value == null) {
            return null;
        }
        return doctors.stream()
                .filter(doctor -> doctor.displayLabel().equals(value))
                .findFirst()
                .orElse(doctors.get(0));
    }

    private DoctorData resolveInitialDoctor() {
        if (initialDoctorId != null) {
            return doctors.stream()
                    .filter(doctor -> doctor.id() == initialDoctorId)
                    .findFirst()
                    .orElseGet(() -> fallbackDoctor());
        }

        if (initialDoctorName == null || initialDoctorName.isBlank()) {
            return doctors.get(0);
        }

        return doctors.stream()
                .filter(doctor -> doctor.name().equals(initialDoctorName)
                        && doctor.speciality().equals(initialDoctorSpeciality)
                        && doctor.address().equals(initialDoctorAddress))
                .findFirst()
                .orElseGet(this::fallbackDoctor);
    }

    private DoctorData fallbackDoctor() {
        return new DoctorData(
                initialDoctorId == null ? 0 : initialDoctorId,
                initialDoctorName == null ? "Aucun médecin" : initialDoctorName,
                initialDoctorSpeciality == null ? "" : initialDoctorSpeciality,
                initialDoctorAddress == null ? "" : initialDoctorAddress,
                ""
        );
    }

    private List<String> buildTimes() {
        List<String> times = new ArrayList<>();
        LocalTime time = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(16, 0);
        while (!time.isAfter(end)) {
            times.add(time.format(TIME_FORMATTER));
            time = time.plusMinutes(30);
        }
        return times;
    }

    private void showSlotUnavailableDialog(Window owner, DoctorData doctor, LocalDate date, String time) {
        showCustomDialog(owner, "Créneau déjà réservé",
                "Le créneau du " + date.format(DATE_FORMATTER) + " à " + time
                        + " pour " + doctor.displayLabel() + " a déjà été réservé.\n\nChoisis un autre créneau.");
    }

    private void showSuccessDialog(Window owner, DoctorData doctor, LocalDate date, String time, ActionEvent event) {
        showCustomDialog(owner, "Rendez-vous confirmé",
                "Ton rendez-vous avec " + doctor.displayLabel() + " est confirmé le "
                        + date.format(DATE_FORMATTER) + " à " + time + ".");
        NavigationUtil.openPatientAppointments(event);
    }

    private void showCustomDialog(Window owner, String title, String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label body = new Label(message);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

        VBox content = new VBox(10, heading, body);
        content.setStyle("-fx-background-color: white; -fx-padding: 20;");

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setStyle("-fx-background-color: white;");
        dialog.showAndWait();
    }

    private Window getWindow(ActionEvent event) {
        return event == null || !(event.getSource() instanceof Node node) || node.getScene() == null
                ? null
                : node.getScene().getWindow();
    }

    private record DoctorData(int id, String name, String speciality, String address, String city) {
        String displayLabel() {
            String safeName = name == null ? "" : name;
            String safeSpeciality = speciality == null ? "" : speciality;
            if (safeName.isBlank()) {
                return safeSpeciality;
            }
            if (safeSpeciality.isBlank()) {
                return safeName;
            }
            return safeName + " - " + safeSpeciality;
        }
    }
}
