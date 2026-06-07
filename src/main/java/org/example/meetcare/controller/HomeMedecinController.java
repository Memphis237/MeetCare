package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HomeMedecinController {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final UsersDao usersDao = new UsersDao();

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label roleBadgeLabel;

    @FXML
    private Label todayAppointmentsCountLabel;

    @FXML
    private Label todayAppointmentsMetaLabel;

    @FXML
    private Label weekAppointmentsCountLabel;

    @FXML
    private Label weekAppointmentsMetaLabel;

    @FXML
    private Label uniquePatientsCountLabel;

    @FXML
    private Label uniquePatientsMetaLabel;

    @FXML
    private Label fillRateCountLabel;

    @FXML
    private Label fillRateMetaLabel;

    @FXML
    private Label todayRangeLabel;

    @FXML
    private VBox todayAppointmentsContainer;

    private List<Appointment> doctorAppointments = List.of();

    @FXML
    private void initialize() {
        refreshDashboard();
    }

    @FXML
    private void handleViewFullAgenda(ActionEvent event) {
        NavigationUtil.openMedecinAgenda(event);
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    private void refreshDashboard() {
        Users currentUser = refreshCurrentUser();
        if (currentUser == null) {
            setEmptyState();
            return;
        }

        String displayName = buildDisplayName(currentUser);
        welcomeLabel.setText("Bienvenue, Dr. " + displayName);
        subtitleLabel.setText("Voici le point du jour sur votre activite clinique.");
        roleBadgeLabel.setText(buildRoleBadge(currentUser));

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);
        todayRangeLabel.setText("Aujourd'hui, " + today.format(DISPLAY_DATE));

        doctorAppointments = appointmentDao.findByDoctorUserId(currentUser.getIdUsers()).stream()
                .sorted(Comparator.comparing(Appointment::getDate).thenComparing(HomeMedecinController::appointmentTime))
                .collect(Collectors.toList());

        List<Appointment> todayAppointments = doctorAppointments.stream()
                .filter(appointment -> appointment.getDate() != null && appointment.getDate().isEqual(today))
                .collect(Collectors.toList());

        List<Appointment> weekAppointments = doctorAppointments.stream()
                .filter(appointment -> appointment.getDate() != null
                        && !appointment.getDate().isBefore(today)
                        && !appointment.getDate().isAfter(weekEnd))
                .collect(Collectors.toList());

        updateStats(todayAppointments, weekAppointments);
        renderTodayAppointments(todayAppointments);
    }

    private void updateStats(List<Appointment> todayAppointments, List<Appointment> weekAppointments) {
        long confirmed = weekAppointments.stream().filter(this::isConfirmed).count();
        long completed = weekAppointments.stream().filter(this::isCompleted).count();
        long cancelled = weekAppointments.stream().filter(this::isCancelled).count();
        long teleconsultations = weekAppointments.stream().filter(Appointment::isTeleconsultation).count();
        long uniquePatients = weekAppointments.stream()
                .map(this::appointmentPatientName)
                .distinct()
                .count();

        int fillRate = weekAppointments.isEmpty()
                ? 0
                : (int) Math.round(((double) Math.max(0, weekAppointments.size() - cancelled) / weekAppointments.size()) * 100.0);

        todayAppointmentsCountLabel.setText(String.valueOf(todayAppointments.size()));
        todayAppointmentsMetaLabel.setText(todayAppointments.isEmpty()
                ? "Aucun rendez-vous aujourd'hui"
                : todayAppointments.size() == 1
                ? "1 consultation prevue"
                : todayAppointments.size() + " consultations prevues");

        weekAppointmentsCountLabel.setText(String.valueOf(weekAppointments.size()));
        weekAppointmentsMetaLabel.setText("Sur les 7 prochains jours");

        uniquePatientsCountLabel.setText(String.valueOf(uniquePatients));
        uniquePatientsMetaLabel.setText(uniquePatients == 1 ? "Patient different" : "Patients differents");

        fillRateCountLabel.setText(fillRate + "%");
        fillRateMetaLabel.setText(
                "Confirmes: " + confirmed + "  |  Termines: " + completed + "  |  Annules: " + cancelled
                        + (teleconsultations > 0 ? "  |  Teleconsultations: " + teleconsultations : "")
        );
    }

    private void renderTodayAppointments(List<Appointment> todayAppointments) {
        if (todayAppointmentsContainer == null) {
            return;
        }

        todayAppointmentsContainer.getChildren().clear();

        if (todayAppointments.isEmpty()) {
            todayAppointmentsContainer.getChildren().add(createEmptyState());
            return;
        }

        for (Appointment appointment : todayAppointments) {
            todayAppointmentsContainer.getChildren().add(createAppointmentCard(appointment));
        }
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(6);
        emptyState.getStyleClass().add("doctor-empty-state");

        Label title = new Label("Aucun rendez-vous aujourd'hui");
        title.getStyleClass().add("doctor-empty-title");

        Label subtitle = new Label("Votre agenda du jour est vide pour le moment. Les prochaines consultations apparaitront ici.");
        subtitle.getStyleClass().add("doctor-empty-subtitle");
        subtitle.setWrapText(true);

        emptyState.getChildren().addAll(title, subtitle);
        return emptyState;
    }

    private HBox createAppointmentCard(Appointment appointment) {
        HBox card = new HBox(16);
        card.getStyleClass().add("doctor-appointment-card");

        VBox slot = new VBox(3);
        slot.setMinWidth(84);

        Label time = new Label(formatTime(appointment.getTime()));
        time.getStyleClass().add("doctor-appointment-time");

        Label day = new Label(shortDate(appointment.getDate()));
        day.getStyleClass().add("doctor-appointment-day");

        slot.getChildren().addAll(time, day);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label patient = new Label(appointmentPatientName(appointment));
        patient.getStyleClass().add("doctor-appointment-name");

        Label speciality = new Label(safeText(appointment.getSpeciality()));
        speciality.getStyleClass().add("doctor-appointment-meta");

        Label details = new Label(safeText(appointment.getDetails()));
        details.getStyleClass().add("doctor-appointment-details");
        details.setWrapText(true);

        info.getChildren().addAll(patient, speciality, details);

        VBox statusBox = new VBox(8);
        statusBox.setMinWidth(170);

        Label status = new Label(displayStatus(appointment.getStatus()));
        status.getStyleClass().add("doctor-status-pill");
        status.setStyle(statusStyle(appointment.getStatus()));

        Label location = new Label(appointment.isTeleconsultation()
                ? "Teleconsultation"
                : safeText(appointment.getAddress()));
        location.getStyleClass().add("doctor-appointment-location");
        location.setWrapText(true);

        Label city = new Label("Ville: " + safeText(appointment.getCity()));
        city.getStyleClass().add("doctor-appointment-meta");

        statusBox.getChildren().addAll(status, location, city);

        card.getChildren().addAll(slot, info, statusBox);
        return card;
    }

    @FXML
    private void showAppointmentDetails(Appointment appointment) {
        if (appointment == null) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Details du rendez-vous");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-pref-width: 420;");

        Label title = new Label(appointmentPatientName(appointment));
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lines = new Label(
                "Date: " + formatDate(appointment.getDate()) + "\n" +
                "Heure: " + formatTime(appointment.getTime()) + "\n" +
                "Statut: " + displayStatus(appointment.getStatus()) + "\n" +
                "Type: " + (appointment.isTeleconsultation() ? "Teleconsultation" : "Cabinet") + "\n" +
                "Motif: " + safeText(appointment.getDetails()) + "\n" +
                "Lieu: " + safeText(appointment.getAddress())
        );
        lines.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 4;");
        lines.setWrapText(true);

        content.getChildren().addAll(title, lines);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private Users refreshCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        Users freshUser = usersDao.findById(user.getIdUsers()).orElse(user);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
    }

    private void setEmptyState() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, Docteur");
        }
        if (subtitleLabel != null) {
            subtitleLabel.setText("Connectez-vous pour voir votre activite du jour.");
        }
        if (roleBadgeLabel != null) {
            roleBadgeLabel.setText("Medecin");
        }
        if (todayRangeLabel != null) {
            todayRangeLabel.setText("Aujourd'hui");
        }
        if (todayAppointmentsCountLabel != null) {
            todayAppointmentsCountLabel.setText("0");
        }
        if (todayAppointmentsMetaLabel != null) {
            todayAppointmentsMetaLabel.setText("Aucun rendez-vous aujourd'hui");
        }
        if (weekAppointmentsCountLabel != null) {
            weekAppointmentsCountLabel.setText("0");
        }
        if (weekAppointmentsMetaLabel != null) {
            weekAppointmentsMetaLabel.setText("Sur les 7 prochains jours");
        }
        if (uniquePatientsCountLabel != null) {
            uniquePatientsCountLabel.setText("0");
        }
        if (uniquePatientsMetaLabel != null) {
            uniquePatientsMetaLabel.setText("Patients differents");
        }
        if (fillRateCountLabel != null) {
            fillRateCountLabel.setText("0%");
        }
        if (fillRateMetaLabel != null) {
            fillRateMetaLabel.setText("Confirmes: 0  |  Termines: 0  |  Annules: 0");
        }
        if (todayAppointmentsContainer != null) {
            todayAppointmentsContainer.getChildren().setAll(createEmptyState());
        }
    }

    private static LocalDate appointmentDate(Appointment appointment) {
        return appointment == null ? null : appointment.getDate();
    }

    private static java.time.LocalTime appointmentTime(Appointment appointment) {
        try {
            return appointment == null || appointment.getTime() == null ? java.time.LocalTime.MIN : java.time.LocalTime.parse(appointment.getTime());
        } catch (Exception ex) {
            return java.time.LocalTime.MIN;
        }
    }

    private String appointmentPatientName(Appointment appointment) {
        String patientName = safeText(appointment == null ? null : appointment.getPatientName());
        if (!"-".equals(patientName)) {
            return patientName;
        }
        return safeText(appointment == null ? null : appointment.getDoctorName());
    }

    private boolean isConfirmed(Appointment appointment) {
        return "CONFIRME".equalsIgnoreCase(safeText(appointment == null ? null : appointment.getStatus()));
    }

    private boolean isCompleted(Appointment appointment) {
        return "TERMINE".equalsIgnoreCase(safeText(appointment == null ? null : appointment.getStatus()));
    }

    private boolean isCancelled(Appointment appointment) {
        return "ANNULE".equalsIgnoreCase(safeText(appointment == null ? null : appointment.getStatus()));
    }

    private String buildDisplayName(Users user) {
        String prenom = safeText(user == null ? null : user.getPrenom());
        String username = safeText(user == null ? null : user.getUsername());

        if ("-".equals(prenom) && "-".equals(username)) {
            return "Medecin";
        }
        if ("-".equals(prenom)) {
            return username;
        }
        if ("-".equals(username)) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private String buildRoleBadge(Users user) {
        if (user == null) {
            return "Medecin";
        }
        if (user.getAccountStatus() == null) {
            return "Medecin";
        }
        return "Medecin - " + user.getAccountStatus().name().toLowerCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String shortDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DISPLAY_DATE);
    }

    private String formatTime(String time) {
        return time == null || time.isBlank() ? "-" : time;
    }

    private String displayStatus(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "Annule";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "Termine";
        }
        return "Confirme";
    }

    private String statusStyle(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #ffedd5; -fx-text-fill: #ea580c;";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb;";
        }
        return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;";
    }
}
