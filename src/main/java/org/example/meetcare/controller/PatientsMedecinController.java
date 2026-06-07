package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.dao.MedicalRecordDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.MedicalRecord;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PatientsMedecinController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsersDao usersDao = new UsersDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final MedicalRecordDao medicalRecordDao = new MedicalRecordDao();

    @FXML
    private TextField searchField;

    @FXML
    private Label filteredCountLabel;

    @FXML
    private VBox patientsListContainer;

    @FXML
    private Label selectedPatientNameLabel;

    @FXML
    private Label selectedPatientMetaLabel;

    @FXML
    private Label selectedPatientBadgeLabel;

    @FXML
    private Label totalAppointmentsValueLabel;

    @FXML
    private Label confirmedAppointmentsValueLabel;

    @FXML
    private Label completedAppointmentsValueLabel;

    @FXML
    private Label dossierStatusLabel;

    @FXML
    private VBox historyContainer;

    private final List<PatientRowData> allPatients = new ArrayList<>();
    private PatientRowData selectedPatient;
    private Users currentUser;

    @FXML
    private void initialize() {
        currentUser = AppSession.getCurrentUser();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        }

        loadPatients();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        applyFilter();
    }

    private void buildPatientList() {
        if (patientsListContainer != null) {
            patientsListContainer.getChildren().clear();
        }
    }

    private void loadPatients() {
        allPatients.clear();

        try {
            List<Users> sourcePatients = loadRelevantPatients();
            for (Users patient : sourcePatients) {
                List<Appointment> appointments = appointmentDao.findByUserId(patient.getIdUsers());
                List<MedicalRecord> records = medicalRecordDao.findByUserId(patient.getIdUsers());
                allPatients.add(new PatientRowData(
                        patient,
                        appointments,
                        records,
                        countByStatus(appointments, "CONFIRME"),
                        countByStatus(appointments, "TERMINE"),
                        appointments.stream()
                                .map(Appointment::getDate)
                                .filter(Objects::nonNull)
                                .max(Comparator.naturalOrder())
                                .orElse(null)
                ));
            }

            allPatients.sort(Comparator
                    .comparing((PatientRowData row) -> row.patient().getPrenom(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(row -> row.patient().getUsername(), String.CASE_INSENSITIVE_ORDER));
            applyFilter();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement", rootCauseMessage(ex));
        }
    }

    private List<Users> loadRelevantPatients() {
        try {
            if (currentUser == null || currentUser.getIdUsers() <= 0) {
                return usersDao.findAllPatients();
            }

            Set<Integer> ids = new LinkedHashSet<>();
            medicalRecordDao.findSharedByDoctorUserId(currentUser.getIdUsers()).forEach(record -> ids.add(record.getIdUsers()));

            if (ids.isEmpty()) {
                return usersDao.findAllPatients();
            }

            List<Users> patients = new ArrayList<>();
            for (Integer id : ids) {
                usersDao.findById(id).ifPresent(patients::add);
            }
            return patients;
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de charger les patients.", ex);
        }
    }

    private void applyFilter() {
        String query = normalize(searchField == null ? "" : searchField.getText());

        List<PatientRowData> filtered = allPatients.stream()
                .filter(row -> query.isBlank()
                        || normalize(row.displayName()).contains(query)
                        || normalize(row.patient().getEmail()).contains(query)
                        || normalize(row.patient().getCity()).contains(query)
                        || normalize(row.patient().getPhone()).contains(query))
                .collect(Collectors.toList());

        if (filteredCountLabel != null) {
            filteredCountLabel.setText(filtered.size() + " patient" + (filtered.size() > 1 ? "s" : ""));
        }

        renderPatientCards(filtered);

        if (filtered.isEmpty()) {
            showEmptyDossier();
            return;
        }

        if (selectedPatient == null || !filtered.contains(selectedPatient)) {
            selectedPatient = filtered.get(0);
        }

        loadPatientDossier(selectedPatient);
        renderPatientCards(filtered);
    }

    private void loadPatientDossier(PatientRowData row) {
        if (row == null) {
            showEmptyDossier();
            return;
        }

        selectedPatient = row;
        selectedPatientNameLabel.setText(row.displayName());
        selectedPatientMetaLabel.setText(buildPatientMeta(row.patient(), row.records().size()));
        selectedPatientBadgeLabel.setText(String.valueOf(row.totalAppointments()));
        totalAppointmentsValueLabel.setText(String.valueOf(row.totalAppointments()));
        confirmedAppointmentsValueLabel.setText(String.valueOf(row.confirmedAppointments()));
        completedAppointmentsValueLabel.setText(String.valueOf(row.completedAppointments()));
        dossierStatusLabel.setText(row.lastAppointmentDate() == null
                ? "Aucun rendez-vous"
                : "Dernier RDV : " + formatDate(row.lastAppointmentDate()));

        renderHistory(row.appointments());
        renderPatientCards(new ArrayList<>(patientsForCurrentFilter()));
    }

    private void renderHistory(List<Appointment> appointments) {
        if (historyContainer == null) {
            return;
        }

        historyContainer.getChildren().clear();
        if (appointments == null || appointments.isEmpty()) {
            historyContainer.getChildren().add(createEmptyHistoryState());
            return;
        }

        List<Appointment> ordered = appointments.stream()
                .sorted(Comparator.comparing(Appointment::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Appointment::getTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        for (Appointment appointment : ordered) {
            historyContainer.getChildren().add(createHistoryCard(appointment));
        }
    }

    private VBox createHistoryCard(Appointment appointment) {
        VBox card = new VBox(6);
        card.getStyleClass().add("doctor-history-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(3);
        Label dateLabel = new Label(formatDate(appointment.getDate()));
        dateLabel.getStyleClass().add("doctor-history-title");
        Label timeLabel = new Label(formatTime(appointment.getTime()));
        timeLabel.getStyleClass().add("doctor-history-meta");
        left.getChildren().addAll(dateLabel, timeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(getStatusLabel(appointment.getStatus()));
        status.setStyle(getStatusStyle(appointment.getStatus()));

        header.getChildren().addAll(left, spacer, status);

        Label notes = new Label("Motif : " + safeText(appointment.getDetails()));
        notes.setWrapText(true);
        notes.getStyleClass().add("doctor-history-meta");

        card.getChildren().addAll(header, notes);
        return card;
    }

    private VBox createEmptyHistoryState() {
        VBox emptyState = new VBox(6);
        emptyState.getStyleClass().add("doctor-empty-state");
        Label title = new Label("Aucun rendez-vous trouvé");
        title.getStyleClass().add("doctor-empty-title");
        Label subtitle = new Label("Ce patient n'a pas encore d'historique visible dans le dossier.");
        subtitle.getStyleClass().add("doctor-empty-subtitle");
        emptyState.getChildren().addAll(title, subtitle);
        return emptyState;
    }

    private void showEmptyDossier() {
        selectedPatient = null;
        selectedPatientNameLabel.setText("Sélectionne un patient");
        selectedPatientMetaLabel.setText("Choisis un patient dans la colonne de gauche pour charger son dossier.");
        selectedPatientBadgeLabel.setText("0");
        totalAppointmentsValueLabel.setText("0");
        confirmedAppointmentsValueLabel.setText("0");
        completedAppointmentsValueLabel.setText("0");
        dossierStatusLabel.setText("Aucun dossier chargé");
        if (historyContainer != null) {
            historyContainer.getChildren().setAll(createEmptyHistoryState());
        }
    }

    private List<PatientRowData> patientsForCurrentFilter() {
        String query = normalize(searchField == null ? "" : searchField.getText());
        return allPatients.stream()
                .filter(row -> query.isBlank()
                        || normalize(row.displayName()).contains(query)
                        || normalize(row.patient().getEmail()).contains(query)
                        || normalize(row.patient().getCity()).contains(query)
                        || normalize(row.patient().getPhone()).contains(query))
                .collect(Collectors.toList());
    }

    private void renderPatientCards(List<PatientRowData> patients) {
        if (patientsListContainer == null) {
            return;
        }

        patientsListContainer.getChildren().clear();
        if (patients.isEmpty()) {
            patientsListContainer.getChildren().add(createEmptyPatientsState());
            return;
        }

        for (PatientRowData patient : patients) {
            patientsListContainer.getChildren().add(createPatientCard(patient));
        }
    }

    private VBox createEmptyPatientsState() {
        VBox empty = new VBox(6);
        empty.getStyleClass().add("doctor-empty-state");
        Label title = new Label("Aucun patient trouvé");
        title.getStyleClass().add("doctor-empty-title");
        Label subtitle = new Label("Essaie un autre nom, une ville ou un contact.");
        subtitle.getStyleClass().add("doctor-empty-subtitle");
        empty.getChildren().addAll(title, subtitle);
        return empty;
    }

    private HBox createPatientCard(PatientRowData patient) {
        boolean active = selectedPatient != null && selectedPatient.equals(patient);

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setOnMouseClicked(event -> loadPatientDossier(patient));
        card.getStyleClass().add("doctor-patient-card");
        if (active) {
            card.getStyleClass().add("doctor-patient-card-active");
        }

        Label badge = new Label(String.valueOf(patient.totalAppointments()));
        badge.getStyleClass().add("doctor-patient-badge");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(patient.displayName());
        name.getStyleClass().add("doctor-patient-name");

        Label rdvCount = new Label(patient.totalAppointments() + " rendez-vous");
        rdvCount.getStyleClass().add("doctor-patient-meta");

        Label lastRdv = new Label("Dernier RDV: " + formatDate(patient.lastAppointmentDate()));
        lastRdv.getStyleClass().add("doctor-patient-meta");

        info.getChildren().addAll(name, rdvCount, lastRdv);
        card.getChildren().addAll(badge, info);
        return card;
    }

    private long countByStatus(List<Appointment> appointments, String status) {
        return appointments.stream()
                .filter(appointment -> status.equalsIgnoreCase(appointment.getStatus()))
                .count();
    }

    private String buildPatientMeta(Users patient, int recordsCount) {
        List<String> parts = new ArrayList<>();
        if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
            parts.add(patient.getEmail().trim());
        }
        if (patient.getCity() != null && !patient.getCity().isBlank()) {
            parts.add(patient.getCity().trim());
        }
        if (patient.getPhone() != null && !patient.getPhone().isBlank()) {
            parts.add(patient.getPhone().trim());
        }
        parts.add(recordsCount + " document" + (recordsCount > 1 ? "s" : ""));
        return String.join(" • ", parts);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMATTER);
    }

    private String formatTime(String time) {
        return time == null || time.isBlank() ? "-" : time;
    }

    private String getStatusLabel(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "Annulé";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "Terminé";
        }
        return "Confirmé";
    }

    private String getStatusStyle(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 999; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-background-radius: 999; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 999; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    private String rootCauseMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? "Une erreur est survenue pendant le chargement."
                : current.getMessage();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record PatientRowData(
            Users patient,
            List<Appointment> appointments,
            List<MedicalRecord> records,
            long confirmedAppointments,
            long completedAppointments,
            LocalDate lastAppointmentDate
    ) {
        private String displayName() {
            String prenom = patient.getPrenom() == null ? "" : patient.getPrenom().trim();
            String username = patient.getUsername() == null ? "" : patient.getUsername().trim();
            if (prenom.isBlank() && username.isBlank()) {
                return "Patient";
            }
            if (prenom.isBlank()) {
                return username;
            }
            if (username.isBlank()) {
                return prenom;
            }
            return prenom + " " + username;
        }

        private int totalAppointments() {
            return appointments == null ? 0 : appointments.size();
        }
    }
}
