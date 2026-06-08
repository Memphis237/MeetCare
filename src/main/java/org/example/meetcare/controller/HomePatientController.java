package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.dao.ChatMessageDao;
import org.example.meetcare.dao.InvoiceDao;
import org.example.meetcare.dao.MedicalRecordDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.Invoice;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;
import org.example.meetcare.utils.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HomePatientController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",
            Locale.FRENCH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicalRecordDao medicalRecordDao = new MedicalRecordDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final ChatMessageDao chatMessageDao = new ChatMessageDao();

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label appointmentsCountLabel;

    @FXML
    private Label documentsCountLabel;

    @FXML
    private Label invoicesCountLabel;

    @FXML
    private Label messagesCountLabel;

    @FXML
    private Label nextAppointmentTitleLabel;

    @FXML
    private Label nextAppointmentSubtitleLabel;

    @FXML
    private Label nextAppointmentDateLabel;

    @FXML
    private Label nextAppointmentStatusLabel;

    @FXML
    private Label nextAppointmentAddressLabel;

    @FXML
    private Button nextAppointmentDetailsButton;

    @FXML
    private VBox upcomingAppointmentsContainer;

    private Appointment selectedUpcomingAppointment;

    @FXML
    private void initialize() {
        try {
            Users user = refreshCurrentUser();
            if (user == null) {
                return;
            }

            String displayName = buildDisplayName(user);
            welcomeLabel.setText("Bienvenue, " + displayName);
            roleLabel.setText(roleToLabel(user.getRole()));

            applyCounterStyles();
            refreshDashboard(user);
            refreshUpcomingAppointments(user);
        } catch (Exception ex) {
            System.err.println("Erreur lors de l'initialisation de l'accueil patient:");
            ex.printStackTrace();
        }
    }

    private void applyCounterStyles() {
        String style = "-fx-font-size: 28px; -fx-font-weight: bold;";
        appointmentsCountLabel.setStyle(style + " -fx-text-fill: #2563eb;");
        documentsCountLabel.setStyle(style + " -fx-text-fill: #7c3aed;");
        invoicesCountLabel.setStyle(style + " -fx-text-fill: #ea580c;");
        messagesCountLabel.setStyle(style + " -fx-text-fill: #16a34a;");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        NavigationUtil.openPatientSettings(event);
    }

    @FXML
    private void handleGoToSearch(MouseEvent event) {
        NavigationUtil.openPatientSearch(event);
    }

    @FXML
    private void handleGoToAppointments(MouseEvent event) {
        NavigationUtil.openPatientAppointments(event);
    }

    @FXML
    private void handleGoToAppointmentsButton(ActionEvent event) {
        NavigationUtil.openPatientAppointments(event);
    }

    @FXML
    private void handleGoToDocuments(MouseEvent event) {
        NavigationUtil.openPatientDocuments(event);
    }

    @FXML
    private void handleGoToInvoices(MouseEvent event) {
        NavigationUtil.openPatientInvoices(event);
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

    private void refreshDashboard(Users user) {
        int userId = user.getIdUsers();
        appointmentsCountLabel.setText(String.valueOf(countUpcomingConfirmedAppointments(userId)));
        documentsCountLabel.setText(String.valueOf(countDocuments(userId)));
        invoicesCountLabel.setText(String.valueOf(countPendingInvoices(userId)));
        messagesCountLabel.setText(String.valueOf(countUnreadMessages(userId)));
    }

    private int countUpcomingConfirmedAppointments(int userId) {
        try {
            return (int) appointmentDao.findByUserId(userId).stream()
                    .filter(appointment -> isUpcomingConfirmed(appointment))
                    .count();
        } catch (Exception ex) {
            return 0;
        }
    }

    private int countDocuments(int userId) {
        try {
            return medicalRecordDao.findByUserId(userId).size();
        } catch (Exception ex) {
            return 0;
        }
    }

    private int countPendingInvoices(int userId) {
        try {
            return (int) invoiceDao.findByPatientUserId(userId).stream()
                    .filter(invoice -> InvoiceStatus.PENDING.equals(invoice.getStatus()))
                    .count();
        } catch (Exception ex) {
            return 0;
        }
    }

    private int countUnreadMessages(int userId) {
        try {
            return chatMessageDao.countUnreadTotalForUser(userId);
        } catch (Exception ex) {
            return 0;
        }
    }

    private void refreshUpcomingAppointments(Users user) {
        if (upcomingAppointmentsContainer == null) {
            return;
        }

        upcomingAppointmentsContainer.getChildren().clear();

        List<Appointment> appointments = appointmentDao.findByUserId(user.getIdUsers()).stream()
                .filter(appointment -> "CONFIRME".equalsIgnoreCase(safeStatus(appointment)))
                .sorted(Comparator.comparing(Appointment::getDate).thenComparing(Appointment::getTime))
                .collect(Collectors.toList());

        if (appointments.isEmpty()) {
            selectedUpcomingAppointment = null;
            setNextAppointmentPlaceholder();

            VBox emptyCard = new VBox(6);
            emptyCard.setStyle("-fx-background-color: #EBF3FB; -fx-background-radius: 12; -fx-padding: 16 16 16 16;");

            Label title = new Label("Aucun rendez-vous confirme");
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A2C42;");

            Label subtitle = new Label("Tes prochains rendez-vous apparaitront ici apres confirmation.");
            subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B8CAD;");

            emptyCard.getChildren().addAll(title, subtitle);
            upcomingAppointmentsContainer.getChildren().add(emptyCard);
            return;
        }

        for (Appointment appointment : appointments) {
            upcomingAppointmentsContainer.getChildren().add(createAppointmentCard(appointment));
        }

        selectedUpcomingAppointment = appointments.stream()
                .filter(this::isUpcomingConfirmed)
                .findFirst()
                .orElse(appointments.get(0));
        renderNextAppointmentCard(selectedUpcomingAppointment);
    }

    private HBox createAppointmentCard(Appointment appointment) {
        HBox card = new HBox(14);
        card.setStyle("-fx-background-color: #EBF3FB; -fx-background-radius: 12; -fx-padding: 16 16 16 16;");

        VBox dateBox = new VBox(2);
        dateBox.setMinWidth(92);
        dateBox.setStyle("-fx-background-color: #D6E9F8; -fx-background-radius: 10; -fx-padding: 10 10 10 10;");

        Label day = new Label(String.valueOf(appointment.getDate().getDayOfMonth()));
        day.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1A2C42;");

        Label month = new Label(appointment.getDate().getMonth().toString().substring(0, 3));
        month.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A6B8A; -fx-font-weight: bold;");

        dateBox.getChildren().addAll(day, month);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(appointment.getDoctorName());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A2C42;");

        Label speciality = new Label(appointment.getSpeciality());
        speciality.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B8CAD;");

        Label date = new Label(formatAppointmentDate(appointment));
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A6B8A;");

        Label location = new Label(appointment.getAddress());
        location.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A6B8A;");

        info.getChildren().addAll(name, speciality, date, location);

        Label status = new Label(statusLabel(appointment.getStatus()));
        status.setStyle(statusStyle(appointment.getStatus()));

        card.getChildren().addAll(dateBox, info, status);
        return card;
    }

    private void renderNextAppointmentCard(Appointment appointment) {
        if (appointment == null) {
            setNextAppointmentPlaceholder();
            return;
        }

        if (nextAppointmentTitleLabel != null) {
            nextAppointmentTitleLabel.setText(appointment.getDoctorName());
        }
        if (nextAppointmentSubtitleLabel != null) {
            nextAppointmentSubtitleLabel.setText(appointment.getSpeciality());
        }
        if (nextAppointmentDateLabel != null) {
            nextAppointmentDateLabel.setText(formatAppointmentDate(appointment));
        }
        if (nextAppointmentStatusLabel != null) {
            nextAppointmentStatusLabel.setText(statusLabel(appointment.getStatus()));
            nextAppointmentStatusLabel.setStyle(statusStyle(appointment.getStatus()));
        }
        if (nextAppointmentAddressLabel != null) {
            nextAppointmentAddressLabel.setText(appointment.getAddress());
        }
        if (nextAppointmentDetailsButton != null) {
            nextAppointmentDetailsButton.setDisable(false);
        }
    }

    private void setNextAppointmentPlaceholder() {
        if (nextAppointmentTitleLabel != null) {
            nextAppointmentTitleLabel.setText("Aucun rendez-vous confirme");
        }
        if (nextAppointmentSubtitleLabel != null) {
            nextAppointmentSubtitleLabel.setText("Tes prochains rendez-vous apparaitront ici.");
        }
        if (nextAppointmentDateLabel != null) {
            nextAppointmentDateLabel.setText("-");
        }
        if (nextAppointmentStatusLabel != null) {
            nextAppointmentStatusLabel.setText("-");
            nextAppointmentStatusLabel.setStyle(statusStyle(null));
        }
        if (nextAppointmentAddressLabel != null) {
            nextAppointmentAddressLabel.setText("-");
        }
        if (nextAppointmentDetailsButton != null) {
            nextAppointmentDetailsButton.setDisable(true);
        }
    }

    @FXML
    private void showAppointmentDetails(Appointment appointment) {
        if (appointment == null) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Details du rendez-vous");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();

        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-pref-width: 420;");

        Label title = new Label(appointment.getDoctorName());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A2C42;");

        Label speciality = new Label(appointment.getSpeciality());
        speciality.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A6B8A;");

        Label date = new Label("Date: " + formatAppointmentDate(appointment));
        date.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A6B8A;");

        Label status = new Label("Statut: " + statusLabel(appointment.getStatus()));
        status.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A6B8A;");

        Label address = new Label("Adresse: " + safeText(appointment.getAddress()));
        address.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A6B8A;");

        Label city = new Label("Ville: " + safeText(appointment.getCity()));
        city.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A6B8A;");

        Label details = new Label("Motif: " + safeText(appointment.getDetails()));
        details.setWrapText(true);
        details.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A6B8A;");

        content.getChildren().addAll(title, speciality, date, status, address, city, details);
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeStatus(Appointment appointment) {
        return appointment == null || appointment.getStatus() == null ? "" : appointment.getStatus().trim();
    }

    private boolean isUpcomingConfirmed(Appointment appointment) {
        if (appointment == null || appointment.getDate() == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        String status = safeStatus(appointment);
        return appointment.getDate().isAfter(today.minusDays(1))
                && "CONFIRME".equalsIgnoreCase(status);
    }

    @FXML
    private void handleNextAppointmentButtonAction() {
        showAppointmentDetails(selectedUpcomingAppointment);
    }

    private String formatAppointmentDate(Appointment appointment) {
        if (appointment == null || appointment.getDate() == null) {
            return "-";
        }
        return appointment.getDate().format(DATE_FORMATTER) + " a "
                + (appointment.getTime() == null ? "" : appointment.getTime());
    }

    private String statusLabel(String status) {
        if ("CONFIRME".equalsIgnoreCase(status)) {
            return "Confirmé";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "Terminé";
        }
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "Annulé";
        }
        return status == null || status.isBlank() ? "Inconnu" : status;
    }

    private String statusStyle(String status) {
        if ("CONFIRME".equalsIgnoreCase(status)) {
            return "-fx-background-color: #D6F0E0; -fx-text-fill: #2A7A4F; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 2 8 2 8;";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 2 8 2 8;";
        }
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 2 8 2 8;";
        }
        return "-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 2 8 2 8;";
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();

        if (prenom.isEmpty() && username.isEmpty()) {
            return "Patient";
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
            return "Patient";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Médecin";
            default -> "Patient";
        };
    }
}
