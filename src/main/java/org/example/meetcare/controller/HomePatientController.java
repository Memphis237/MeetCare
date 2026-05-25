package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.PatientCareDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class HomePatientController {
    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitialsLabel;
    @FXML private Circle mainAvatarCircle;
    @FXML private Label mainAvatarInitialsLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label appointmentsCountLabel;
    @FXML private Label documentsCountLabel;
    @FXML private Label invoicesCountLabel;
    @FXML private Label messagesCountLabel;
    @FXML private Label nextAppointmentDoctorLabel;
    @FXML private Label nextAppointmentSpecialityLabel;
    @FXML private Label nextAppointmentStatusLabel;
    @FXML private Label nextAppointmentDateLabel;
    @FXML private Label nextAppointmentAddressLabel;
    @FXML private VBox notificationsContainer;

    private final PatientCareDao patientCareDao = new PatientCareDao();

    @FXML
    private void initialize() {
        Users user = refreshCurrentUser();
        if (user == null) {
            return;
        }

        String displayName = buildDisplayName(user);
        if (patientNameLabel != null) {
            patientNameLabel.setText(displayName);
        }
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + displayName);
        }
        if (roleLabel != null) {
            roleLabel.setText(roleToLabel(user.getRole()));
        }
        if (avatarCircle != null && avatarInitialsLabel != null) {
            AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, user);
        }
        if (mainAvatarCircle != null && mainAvatarInitialsLabel != null) {
            AvatarUtil.applyAvatar(mainAvatarCircle, mainAvatarInitialsLabel, user);
        }

        bindDashboardData();
    }

    private void bindDashboardData() {
        PatientCareDao.PatientDashboardData dashboardData = patientCareDao.getPatientDashboardData();

        if (appointmentsCountLabel != null) {
            appointmentsCountLabel.setText(String.valueOf(dashboardData.upcomingAppointments()));
        }
        if (documentsCountLabel != null) {
            documentsCountLabel.setText(String.valueOf(dashboardData.documentsCount()));
        }
        if (invoicesCountLabel != null) {
            invoicesCountLabel.setText(String.valueOf(dashboardData.pendingInvoices()));
        }
        if (messagesCountLabel != null) {
            messagesCountLabel.setText(String.valueOf(dashboardData.unreadMessages()));
        }

        Appointment nextAppointment = dashboardData.nextAppointment();
        if (nextAppointment == null) {
            nextAppointmentDoctorLabel.setText("Aucun rendez-vous prevu");
            nextAppointmentSpecialityLabel.setText("Prenez un rendez-vous depuis la recherche de medecin.");
            nextAppointmentStatusLabel.setText("A venir");
            nextAppointmentDateLabel.setText("Date a definir");
            nextAppointmentAddressLabel.setText("Choisissez un creneau disponible.");
        } else {
            nextAppointmentDoctorLabel.setText(nextAppointment.getDoctorName());
            nextAppointmentSpecialityLabel.setText(nextAppointment.getSpeciality());
            nextAppointmentStatusLabel.setText("Confirme");
            nextAppointmentDateLabel.setText(formatDate(nextAppointment) + " a " + nextAppointment.getTime());
            nextAppointmentAddressLabel.setText(nextAppointment.getAddress());
        }

        notificationsContainer.getChildren().clear();
        for (PatientCareDao.DashboardNotification notification : dashboardData.notifications()) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-padding: 14;");

            Label title = new Label(notification.title());
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            Label content = new Label(notification.content());
            content.setWrapText(true);
            content.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            card.getChildren().addAll(title, content);
            notificationsContainer.getChildren().add(card);
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/settingsPatient.fxml", 1100, 700, "MeetCare - Parametres");
    }

    @FXML
    private void handleOpenSearchDoctor(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/searchDoctor.fxml", 1200, 800, "MeetCare - Recherche");
    }

    @FXML
    private void handleOpenAppointments(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/myAppointments.fxml", 1200, 720, "MeetCare - Mes rendez-vous");
    }

    @FXML
    private void handleOpenDocuments(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/documentsPatient.fxml", 1200, 800, "MeetCare - Documents");
    }

    @FXML
    private void handleOpenInvoices(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/invoicePatient.fxml", 1100, 700, "MeetCare - Factures");
    }

    @FXML
    private void handleOpenMessages(MouseEvent event) {
        NavigationUtil.openPatientMessages(event);
    }

    @FXML
    private void handleOpenAssistant(MouseEvent event) {
        NavigationUtil.openPatientMessages(event);
    }

    @FXML
    private void handleOpenNotifications(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/notificationsPatient.fxml", 1100, 700, "MeetCare - Notifications");
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
            case MEDECIN -> "Medecin";
            default -> "Patient";
        };
    }

    private String formatDate(Appointment appointment) {
        return appointment.getDate().getDayOfMonth() + "/" + appointment.getDate().getMonthValue() + "/" + appointment.getDate().getYear();
    }
}
