package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import org.example.meetcare.dao.MedicalRecordDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;

public class HomePatientController {
    private final MedicalRecordDao medicalRecordDao = new MedicalRecordDao();

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label documentsCountLabel;

    @FXML
    private void initialize() {
        try {
            Users user = refreshCurrentUser();
            if (user == null) {
                return;
            }

            String displayName = buildDisplayName(user);
            patientNameLabel.setText(displayName);
            welcomeLabel.setText("Bienvenue, " + displayName);
            roleLabel.setText(roleToLabel(user.getRole()));
            refreshDocumentsCount(user);
        } catch (Exception ex) {
            System.err.println("Erreur lors de l'initialisation de l'accueil patient:");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    @FXML
    private void handleSettings(MouseEvent event) {
        NavigationUtil.navigateTo(event, "/view/settingsPatient.fxml", 1100, 700, "MeetCare - ParamÃ¨tres");
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

    private void refreshDocumentsCount(Users user) {
        if (documentsCountLabel == null || user == null || user.getIdUsers() <= 0) {
            return;
        }

        try {
            documentsCountLabel.setText(String.valueOf(medicalRecordDao.findByUserId(user.getIdUsers()).size()));
        } catch (Exception ex) {
            documentsCountLabel.setText("0");
        }
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
