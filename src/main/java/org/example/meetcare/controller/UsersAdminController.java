package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class UsersAdminController {
    private final UsersDao usersDao = new UsersDao();

    @FXML
    private VBox contentContainer;

    private TextField searchField;
    private Button allFilterButton;
    private Button patientFilterButton;
    private Button doctorFilterButton;
    private Button adminFilterButton;
    private Label totalCountLabel;
    private Label patientsCountLabel;
    private Label doctorsCountLabel;
    private Label adminsCountLabel;
    private VBox cardsContainer;

    private List<Users> users = List.of();
    private String activeFilter = "ALL";

    @FXML
    private void initialize() {
        buildLayout();
        refreshData();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshData();
    }

    @FXML
    private void handleAllFilter(ActionEvent event) {
        activeFilter = "ALL";
        refreshCards();
    }

    @FXML
    private void handlePatientFilter(ActionEvent event) {
        activeFilter = "USER";
        refreshCards();
    }

    @FXML
    private void handleDoctorFilter(ActionEvent event) {
        activeFilter = "MEDECIN";
        refreshCards();
    }

    @FXML
    private void handleAdminFilter(ActionEvent event) {
        activeFilter = "ADMIN";
        refreshCards();
    }

    private void buildLayout() {
        if (contentContainer == null) {
            return;
        }

        contentContainer.getChildren().clear();
        contentContainer.setSpacing(24);
        contentContainer.setPadding(new Insets(0));

        contentContainer.getChildren().addAll(
                buildHeader(),
                buildStatsRow(),
                buildFilterCard(),
                buildCardsContainer()
        );
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(6);
        Label title = new Label("Gestion des utilisateurs");
        title.getStyleClass().add("admin-page-title");

        Label subtitle = new Label("Pilote les comptes patients, médecins et administrateurs");
        subtitle.getStyleClass().add("admin-page-subtitle");
        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Actualiser");
        refreshButton.getStyleClass().add("admin-refresh-button");
        refreshButton.setOnAction(this::handleRefresh);

        header.getChildren().addAll(texts, spacer, refreshButton);
        return header;
    }

    private HBox buildStatsRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                createStatCard("Total utilisateurs", "0", "admin-stat-card-completed", "admin-stat-title-completed", "admin-stat-value-completed", "👥"),
                createStatCard("Patients", "0", "admin-stat-card-confirmed", "admin-stat-title-confirmed", "admin-stat-value-confirmed", "🙂"),
                createStatCard("Médecins", "0", "admin-stat-card-urgent", "admin-stat-title-urgent", "admin-stat-value-urgent", "🩺"),
                createStatCard("Admins", "0", "admin-stat-card-cancelled", "admin-stat-title-cancelled", "admin-stat-value-cancelled", "⚙")
        );
        return row;
    }

    private VBox createStatCard(String titleText,
                                String initialValue,
                                String cardStyle,
                                String titleStyle,
                                String valueStyle,
                                String iconText) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("admin-stat-card", cardStyle);
        card.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size: 22px;");

        Label title = new Label(titleText);
        title.getStyleClass().addAll("admin-stat-title", titleStyle);

        Label value = new Label(initialValue);
        value.getStyleClass().addAll("admin-stat-value", valueStyle);

        switch (titleText) {
            case "Total utilisateurs" -> totalCountLabel = value;
            case "Patients" -> patientsCountLabel = value;
            case "Médecins" -> doctorsCountLabel = value;
            case "Admins" -> adminsCountLabel = value;
            default -> {
            }
        }

        card.getChildren().addAll(icon, title, value);
        return card;
    }

    private VBox buildFilterCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("doctors-admin-filter-card");

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Rechercher un utilisateur...");
        searchField.getStyleClass().add("doctors-admin-search");
        searchField.setPrefWidth(520);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshCards());

        allFilterButton = createFilterButton("Tous", this::handleAllFilter);
        patientFilterButton = createFilterButton("Patients", this::handlePatientFilter);
        doctorFilterButton = createFilterButton("Médecins", this::handleDoctorFilter);
        adminFilterButton = createFilterButton("Admins", this::handleAdminFilter);

        row.getChildren().addAll(searchField, allFilterButton, patientFilterButton, doctorFilterButton, adminFilterButton);
        card.getChildren().add(row);
        return card;
    }

    private VBox buildCardsContainer() {
        cardsContainer = new VBox(16);
        return cardsContainer;
    }

    private Button createFilterButton(String text, javafx.event.EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.setOnAction(action);
        button.setMinWidth(140);
        button.setPrefHeight(56);
        button.setStyle(baseFilterStyle(false));
        return button;
    }

    private void refreshData() {
        users = usersDao.findAllUsers().stream()
                .sorted(Comparator.comparing((Users user) -> roleRank(user.getRole()))
                        .thenComparing(this::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        updateStats();
        refreshCards();
    }

    private void updateStats() {
        totalCountLabel.setText(String.valueOf(users.size()));
        patientsCountLabel.setText(String.valueOf(countByRole(Role.USER)));
        doctorsCountLabel.setText(String.valueOf(countByRole(Role.MEDECIN)));
        adminsCountLabel.setText(String.valueOf(countByRole(Role.ADMIN)));
    }

    private long countByRole(Role role) {
        return users.stream().filter(user -> user.getRole() == role).count();
    }

    private void refreshCards() {
        if (cardsContainer == null) {
            return;
        }

        cardsContainer.getChildren().clear();
        updateFilterStyles();

        List<Users> filtered = users.stream()
                .filter(this::matchesActiveFilter)
                .filter(this::matchesSearch)
                .toList();

        if (filtered.isEmpty()) {
            VBox empty = new VBox(6);
            empty.getStyleClass().add("admin-table-empty");
            Label title = new Label("Aucun utilisateur trouvé");
            title.getStyleClass().add("admin-empty-title");
            Label subtitle = new Label("Modifie le filtre ou la recherche.");
            subtitle.getStyleClass().add("admin-empty-subtitle");
            empty.getChildren().addAll(title, subtitle);
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (Users user : filtered) {
            cardsContainer.getChildren().add(createUserCard(user));
        }
    }

    private HBox createUserCard(Users user) {
        HBox card = new HBox(18);
        card.getStyleClass().add("doctor-admin-card");
        card.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label(initialsOf(displayName(user)));
        avatar.getStyleClass().add("doctor-admin-avatar");
        avatar.setStyle("-fx-background-color: " + avatarColor(user.getRole()) + "; -fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-min-width: 60; -fx-min-height: 60; -fx-alignment: center; -fx-background-radius: 30;");

        VBox details = new VBox(8);
        HBox header = new HBox();
        VBox nameBlock = new VBox(2);

        Label name = new Label(displayName(user));
        name.getStyleClass().add("doctor-admin-name");

        Label role = new Label(roleLabel(user.getRole()));
        role.getStyleClass().add("doctor-admin-speciality");

        nameBlock.getChildren().addAll(name, role);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);

        Label status = new Label(displayStatus(user.getAccountStatus()));
        status.getStyleClass().add(statusStyle(user.getAccountStatus()));
        header.getChildren().addAll(nameBlock, status);

        Label meta1 = new Label("Email: " + safeText(user.getEmail()) + "   |   Tél: " + safeText(user.getPhone()));
        meta1.getStyleClass().add("doctor-admin-meta");

        Label meta2 = new Label("Ville: " + safeText(user.getCity()) + "   |   Adresse: " + safeText(user.getAddress()));
        meta2.getStyleClass().add("doctor-admin-meta");

        HBox actions = new HBox(10);
        Button verify = new Button("Valider");
        verify.getStyleClass().add("doctor-admin-action-validate");
        verify.setOnAction(event -> handleSetStatus(user, AccountStatus.VERIFIED));

        Button suspend = new Button("Suspendre");
        suspend.getStyleClass().add("doctor-admin-action-reject");
        suspend.setOnAction(event -> handleSetStatus(user, AccountStatus.SUSPENDED));

        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("doctor-admin-action-reject");
        delete.setOnAction(event -> handleDelete(user));

        actions.getChildren().addAll(verify, suspend, delete);

        details.getChildren().addAll(header, meta1, meta2, actions);
        HBox.setHgrow(details, Priority.ALWAYS);

        card.getChildren().addAll(avatar, details);
        return card;
    }

    private void handleSetStatus(Users user, AccountStatus status) {
        Users admin = AppSession.getCurrentUser();
        if (admin == null) {
            showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun administrateur connecté.");
            return;
        }

        if (user.getIdUsers() == admin.getIdUsers()) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Tu ne peux pas modifier ton propre compte ici.");
            return;
        }

        usersDao.updateAccountStatus(user.getIdUsers(), status.name());
        refreshData();
        showAlert(Alert.AlertType.INFORMATION, "Mise à jour effectuée", displayName(user) + " est maintenant " + displayStatus(status).toLowerCase(Locale.ROOT) + ".");
    }

    private void handleDelete(Users user) {
        Users admin = AppSession.getCurrentUser();
        if (admin == null) {
            showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun administrateur connecté.");
            return;
        }

        if (user.getIdUsers() == admin.getIdUsers()) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Tu ne peux pas supprimer ton propre compte depuis cette interface.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Suppression utilisateur");
        confirmation.setHeaderText("Supprimer " + displayName(user) + " ?");
        confirmation.setContentText("Cette action supprimera le compte et les données liées.");
        ButtonType deleteButton = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(deleteButton, cancelButton);

        confirmation.showAndWait().ifPresent(result -> {
            if (result == deleteButton) {
                usersDao.deleteById(user.getIdUsers());
                refreshData();
            }
        });
    }

    private boolean matchesActiveFilter(Users user) {
        return switch (activeFilter) {
            case "USER" -> user.getRole() == Role.USER;
            case "MEDECIN" -> user.getRole() == Role.MEDECIN;
            case "ADMIN" -> user.getRole() == Role.ADMIN;
            default -> true;
        };
    }

    private boolean matchesSearch(Users user) {
        String query = searchField == null ? "" : searchField.getText();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return true;
        }

        return displayName(user).toLowerCase(Locale.ROOT).contains(normalized)
                || safeText(user.getEmail()).toLowerCase(Locale.ROOT).contains(normalized)
                || safeText(user.getCity()).toLowerCase(Locale.ROOT).contains(normalized)
                || roleLabel(user.getRole()).toLowerCase(Locale.ROOT).contains(normalized);
    }

    private void updateFilterStyles() {
        allFilterButton.setStyle(baseFilterStyle("ALL".equals(activeFilter)));
        patientFilterButton.setStyle(baseFilterStyle("USER".equals(activeFilter)));
        doctorFilterButton.setStyle(baseFilterStyle("MEDECIN".equals(activeFilter)));
        adminFilterButton.setStyle(baseFilterStyle("ADMIN".equals(activeFilter)));
    }

    private String baseFilterStyle(boolean active) {
        return active
                ? "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 16; -fx-font-weight: bold; -fx-font-size: 15px;"
                : "-fx-background-color: #f3f4f6; -fx-text-fill: #334155; -fx-background-radius: 16; -fx-font-weight: bold; -fx-font-size: 15px;";
    }

    private String displayName(Users user) {
        String prenom = safeText(user.getPrenom());
        String username = safeText(user.getUsername());
        if ("-".equals(prenom) && "-".equals(username)) {
            return "Utilisateur";
        }
        if ("-".equals(prenom)) {
            return username;
        }
        if ("-".equals(username)) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private String roleLabel(Role role) {
        if (role == null) {
            return "Utilisateur";
        }
        return switch (role) {
            case ADMIN -> "Administrateur";
            case MEDECIN -> "Médecin";
            default -> "Patient";
        };
    }

    private String displayStatus(AccountStatus status) {
        if (status == AccountStatus.VERIFIED) {
            return "Validé";
        }
        if (status == AccountStatus.REJECTED) {
            return "Refusé";
        }
        if (status == AccountStatus.SUSPENDED) {
            return "Suspendu";
        }
        return "En attente";
    }

    private String statusStyle(AccountStatus status) {
        if (status == AccountStatus.VERIFIED) {
            return "doctor-admin-status-valid";
        }
        if (status == AccountStatus.REJECTED) {
            return "doctor-admin-status-rejected";
        }
        if (status == AccountStatus.SUSPENDED) {
            return "doctor-admin-status-rejected";
        }
        return "doctor-admin-status-pending";
    }

    private int roleRank(Role role) {
        if (role == Role.ADMIN) {
            return 0;
        }
        if (role == Role.MEDECIN) {
            return 1;
        }
        return 2;
    }

    private String initialsOf(String value) {
        if (value == null || value.isBlank()) {
            return "U";
        }
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String avatarColor(Role role) {
        return switch (role == null ? Role.USER : role) {
            case ADMIN -> "#7c3aed";
            case MEDECIN -> "#0ea5e9";
            default -> "#10b981";
        };
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
