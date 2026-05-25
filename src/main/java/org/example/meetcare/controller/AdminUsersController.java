package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

import java.util.List;
import java.util.Locale;

public class AdminUsersController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label usersCountLabel;
    @FXML private VBox usersContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("users");
        statusFilterComboBox.getItems().setAll("Tous les statuts", "Actif", "Inactif");
        statusFilterComboBox.setValue("Tous les statuts");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshUsers());
        statusFilterComboBox.setOnAction(event -> refreshUsers());
        refreshUsers();
    }

    private void refreshUsers() {
        String query = normalize(searchField.getText());
        String status = statusFilterComboBox.getValue();

        List<AdminWorkspaceDao.AdminUserRecord> users = adminWorkspaceDao.getUsers().stream()
                .filter(user -> query.isBlank() || normalize(user.name()).contains(query) || normalize(user.email()).contains(query))
                .filter(user -> "Tous les statuts".equals(status) || matchesUserStatus(user.status(), status))
                .toList();

        usersCountLabel.setText(users.size() + (users.size() > 1 ? " utilisateur(s)" : " utilisateur"));
        usersContainer.getChildren().clear();

        for (AdminWorkspaceDao.AdminUserRecord user : users) {
            usersContainer.getChildren().add(buildUserCard(user));
        }
    }

    private VBox buildUserCard(AdminWorkspaceDao.AdminUserRecord user) {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("admin-list-item");

        HBox row = new HBox(16);
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("admin-avatar");
        Label initials = new Label(user.initials());
        initials.getStyleClass().add("admin-avatar-label");
        avatar.getChildren().add(initials);

        VBox details = new VBox(6);
        HBox top = new HBox(10);
        Label name = new Label(user.name());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        Label badge = new Label(displayStatus(user.status()));
        badge.getStyleClass().addAll("admin-badge", isActive(user.status()) ? "admin-badge-active" : "admin-badge-danger");
        top.getChildren().addAll(name, badge);

        Label email = new Label(user.email());
        email.getStyleClass().add("admin-muted");
        Label meta = new Label(user.appointmentsCount() + " rendez-vous  •  Dernier RDV: " + user.lastAppointment());
        meta.getStyleClass().add("admin-muted");
        details.getChildren().addAll(top, email, meta);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button viewButton = buildButton("Voir profil", "admin-secondary-button");
        viewButton.setOnAction(event -> showUserProfile(user));

        Button toggleButton = buildButton(isActive(user.status()) ? "Désactiver" : "Activer",
                isActive(user.status()) ? "admin-danger-button" : "admin-success-button");
        toggleButton.setOnAction(event -> {
            adminWorkspaceDao.updateUserStatus(user.id(), isActive(user.status()) ? "INACTIVE" : "ACTIVE");
            refreshUsers();
        });

        Button deleteButton = buildButton("Supprimer", "admin-danger-button");
        deleteButton.setOnAction(event -> {
            adminWorkspaceDao.deleteUser(user.id());
            refreshUsers();
        });

        actions.getChildren().addAll(viewButton, toggleButton, deleteButton);
        row.getChildren().addAll(avatar, details, actions);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private void showUserProfile(AdminWorkspaceDao.AdminUserRecord user) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Profil utilisateur");
        alert.setHeaderText(user.name());
        alert.setContentText(
                "Email: " + user.email() + "\n" +
                "Téléphone: " + user.phone() + "\n" +
                "Statut: " + displayStatus(user.status()) + "\n" +
                "Rendez-vous: " + user.appointmentsCount() + "\n" +
                "Dernier RDV: " + user.lastAppointment()
        );
        alert.showAndWait();
    }

    private Button buildButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private boolean matchesUserStatus(String rawStatus, String selected) {
        return displayStatus(rawStatus).equalsIgnoreCase(selected);
    }

    private boolean isActive(String status) {
        return "ACTIVE".equalsIgnoreCase(status) || "Actif".equalsIgnoreCase(status);
    }

    private String displayStatus(String status) {
        return isActive(status) ? "Actif" : "Inactif";
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.FRENCH).trim();
    }
}
