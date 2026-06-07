package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.DoctorAdminEntry;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DoctorsAdminController {
    private static final NumberFormat PRICE_FORMAT = NumberFormat.getNumberInstance(Locale.FRANCE);

    private final DoctorDao doctorDao = new DoctorDao();

    @FXML
    private VBox contentContainer;

    private TextField searchField;
    private Button allFilterButton;
    private Button validatedFilterButton;
    private Button pendingFilterButton;
    private Label totalCountLabel;
    private Label validatedCountLabel;
    private Label pendingCountLabel;
    private VBox cardsContainer;

    private List<DoctorAdminEntry> allDoctors = List.of();
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
        Label title = new Label("Gestion des médecins");
        title.getStyleClass().add("admin-page-title");

        Label subtitle = new Label("Valider et gérer les comptes des professionnels de santé");
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
                createStatCard("Total médecins", "0", "admin-stat-card-completed", "admin-stat-title-completed", "admin-stat-value-completed", "⌘"),
                createStatCard("Validés", "0", "admin-stat-card-confirmed", "admin-stat-title-confirmed", "admin-stat-value-confirmed", "✓"),
                createStatCard("En attente", "0", "admin-stat-card-urgent", "admin-stat-title-urgent", "admin-stat-value-urgent", "⨯")
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
        icon.setFont(Font.font(20));

        Label title = new Label(titleText);
        title.getStyleClass().addAll("admin-stat-title", titleStyle);

        Label value = new Label(initialValue);
        value.getStyleClass().addAll("admin-stat-value", valueStyle);

        switch (titleText) {
            case "Total médecins" -> totalCountLabel = value;
            case "Validés" -> validatedCountLabel = value;
            case "En attente" -> pendingCountLabel = value;
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
        searchField.setPromptText("Rechercher un médecin...");
        searchField.getStyleClass().add("doctors-admin-search");
        searchField.setPrefWidth(520);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshCards());

        allFilterButton = createFilterButton("Tous", this::handleAllFilter);
        validatedFilterButton = createFilterButton("Validés", this::handleValidatedFilter);
        pendingFilterButton = createFilterButton("En attente", this::handlePendingFilter);

        row.getChildren().addAll(searchField, allFilterButton, validatedFilterButton, pendingFilterButton);
        card.getChildren().add(row);
        return card;
    }

    private VBox buildCardsContainer() {
        cardsContainer = new VBox(16);
        cardsContainer.getStyleClass().add("doctors-admin-list");
        return cardsContainer;
    }

    private Button createFilterButton(String text, javafx.event.EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.setOnAction(action);
        button.setMinWidth(170);
        button.setPrefHeight(56);
        button.setStyle(baseFilterStyle(false));
        return button;
    }

    @FXML
    private void handleAllFilter(ActionEvent event) {
        activeFilter = "ALL";
        refreshCards();
    }

    @FXML
    private void handleValidatedFilter(ActionEvent event) {
        activeFilter = "VERIFIED";
        refreshCards();
    }

    @FXML
    private void handlePendingFilter(ActionEvent event) {
        activeFilter = "PENDING";
        refreshCards();
    }

    private void refreshData() {
        allDoctors = doctorDao.findAllAdminEntries().stream()
                .sorted(Comparator.comparing(this::sortStatusRank)
                        .thenComparing(this::doctorDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        updateStats();
        refreshCards();
    }

    private void updateStats() {
        totalCountLabel.setText(String.valueOf(allDoctors.size()));
        validatedCountLabel.setText(String.valueOf(countByStatus(AccountStatus.VERIFIED)));
        pendingCountLabel.setText(String.valueOf(countByStatus(AccountStatus.PENDING)));
    }

    private long countByStatus(AccountStatus status) {
        return allDoctors.stream().filter(doctor -> doctor.getAccountStatus() == status).count();
    }

    private void refreshCards() {
        if (cardsContainer == null) {
            return;
        }

        cardsContainer.getChildren().clear();
        updateFilterStyles();

        List<DoctorAdminEntry> filtered = allDoctors.stream()
                .filter(this::matchesActiveFilter)
                .filter(this::matchesSearch)
                .toList();

        if (filtered.isEmpty()) {
            VBox empty = new VBox(6);
            empty.getStyleClass().add("admin-table-empty");
            Label title = new Label("Aucun médecin trouvé");
            title.getStyleClass().add("admin-empty-title");
            Label subtitle = new Label("Essaie avec un autre filtre ou un autre mot-clé.");
            subtitle.getStyleClass().add("admin-empty-subtitle");
            empty.getChildren().addAll(title, subtitle);
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (DoctorAdminEntry doctor : filtered) {
            cardsContainer.getChildren().add(createDoctorCard(doctor));
        }
    }

    private HBox createDoctorCard(DoctorAdminEntry doctor) {
        HBox card = new HBox(18);
        card.getStyleClass().add("doctor-admin-card");
        card.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label(initialsOf(doctorDisplayName(doctor)));
        avatar.getStyleClass().add("doctor-admin-avatar");

        VBox details = new VBox(8);
        HBox header = new HBox();
        VBox nameBlock = new VBox(2);

        Label name = new Label(doctorDisplayName(doctor));
        name.getStyleClass().add("doctor-admin-name");

        Label speciality = new Label(safeText(doctor.getSpeciality()));
        speciality.getStyleClass().add("doctor-admin-speciality");

        nameBlock.getChildren().addAll(name, speciality);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);

        Label status = new Label(displayStatus(doctor.getAccountStatus()));
        status.getStyleClass().add(statusStyle(doctor.getAccountStatus()));
        header.getChildren().addAll(nameBlock, status);

        Label bio = new Label(safeText(doctor.getBiography(), "Aucune biographie renseignée."));
        bio.setWrapText(true);
        bio.getStyleClass().add("doctor-admin-bio");

        HBox metaRow = new HBox(24);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getChildren().addAll(
                new Label("📍 " + buildLocation(doctor)),
                new Label("€ " + formatPrice(doctor.getConsultationPrice()) + " / consultation"),
                new Label(experienceText(doctor))
        );
        for (var node : metaRow.getChildren()) {
            if (node instanceof Label label) {
                label.getStyleClass().add("doctor-admin-meta");
            }
        }

        Label contact = new Label("Email: " + safeText(doctor.getEmail()) + "   |   Tél: " + safeText(doctor.getPhone()));
        contact.getStyleClass().add("doctor-admin-meta");

        HBox badges = new HBox(10);
        badges.getChildren().addAll(
                createMiniBadge(doctor.isTeleconsultation() ? "Téléconsultation" : "Cabinet"),
                createMiniBadge(doctor.isAcceptsVitale() ? "Carte Vitale" : "Sans Vitale"),
                createMiniBadge(doctor.isPublicProfileCompleted() ? "Profil public" : "Profil incomplet")
        );

        HBox actions = new HBox(10);
        Button validate = new Button("Valider");
        validate.getStyleClass().add("doctor-admin-action-validate");
        validate.setOnAction(event -> handleValidate(doctor));

        Button reject = new Button("Refuser");
        reject.getStyleClass().add("doctor-admin-action-reject");
        reject.setOnAction(event -> handleReject(doctor));

        actions.getChildren().addAll(validate, reject);

        details.getChildren().addAll(header, bio, metaRow, contact, badges, actions);
        HBox.setHgrow(details, Priority.ALWAYS);

        card.getChildren().addAll(avatar, details);
        return card;
    }

    private Label createMiniBadge(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #334155; -fx-background-radius: 999; -fx-padding: 6 10 6 10; -fx-font-size: 12px;");
        return label;
    }

    private void handleValidate(DoctorAdminEntry doctor) {
        Users admin = AppSession.getCurrentUser();
        if (admin == null) {
            showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun administrateur connecté.");
            return;
        }

        doctorDao.saveVerificationDecision(doctor.getDoctorId(), admin.getIdUsers(), "APPROVED", "Validation du compte médecin");
        new org.example.meetcare.dao.UsersDao().updateAccountStatus(doctor.getUserId(), AccountStatus.VERIFIED.name());
        refreshData();
        showAlert(Alert.AlertType.INFORMATION, "Médecin validé", doctorDisplayName(doctor) + " a été validé avec succès.");
    }

    private void handleReject(DoctorAdminEntry doctor) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refus du compte");
        dialog.setHeaderText("Indique la raison du refus");
        dialog.setContentText("Motif :");

        dialog.showAndWait().ifPresent(reason -> {
            String trimmed = reason == null ? "" : reason.trim();
            if (trimmed.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Motif requis", "Merci de saisir une raison.");
                return;
            }

            Users admin = AppSession.getCurrentUser();
            if (admin == null) {
                showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun administrateur connecté.");
                return;
            }

            doctorDao.saveVerificationDecision(doctor.getDoctorId(), admin.getIdUsers(), "REJECTED", trimmed);
            new org.example.meetcare.dao.UsersDao().updateAccountStatus(doctor.getUserId(), AccountStatus.REJECTED.name());
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Médecin refusé", doctorDisplayName(doctor) + " a été refusé.");
        });
    }

    private boolean matchesActiveFilter(DoctorAdminEntry doctor) {
        return switch (activeFilter) {
            case "VERIFIED" -> doctor.getAccountStatus() == AccountStatus.VERIFIED;
            case "PENDING" -> doctor.getAccountStatus() == AccountStatus.PENDING;
            default -> true;
        };
    }

    private boolean matchesSearch(DoctorAdminEntry doctor) {
        String query = searchField == null ? "" : searchField.getText();
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return true;
        }

        return doctorDisplayName(doctor).toLowerCase(Locale.ROOT).contains(normalized)
                || safeText(doctor.getSpeciality()).toLowerCase(Locale.ROOT).contains(normalized)
                || safeText(doctor.getEmail()).toLowerCase(Locale.ROOT).contains(normalized)
                || safeText(doctor.getCity()).toLowerCase(Locale.ROOT).contains(normalized);
    }

    private void updateFilterStyles() {
        allFilterButton.setStyle(baseFilterStyle("ALL".equals(activeFilter)));
        validatedFilterButton.setStyle(baseFilterStyle("VERIFIED".equals(activeFilter)));
        pendingFilterButton.setStyle(baseFilterStyle("PENDING".equals(activeFilter)));
    }

    private String baseFilterStyle(boolean active) {
        return active
                ? "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 16; -fx-font-weight: bold; -fx-font-size: 15px;"
                : "-fx-background-color: #f3f4f6; -fx-text-fill: #334155; -fx-background-radius: 16; -fx-font-weight: bold; -fx-font-size: 15px;";
    }

    private String doctorDisplayName(DoctorAdminEntry doctor) {
        String prenom = safeText(doctor.getPrenom(), "");
        String username = safeText(doctor.getUsername(), "");
        if (prenom.isBlank() && username.isBlank()) {
            return "Dr. Médecin";
        }
        if (prenom.isBlank()) {
            return "Dr. " + username;
        }
        if (username.isBlank()) {
            return "Dr. " + prenom;
        }
        return "Dr. " + prenom + " " + username;
    }

    private String displayStatus(AccountStatus status) {
        if (status == AccountStatus.VERIFIED) {
            return "Validé";
        }
        if (status == AccountStatus.REJECTED) {
            return "Refusé";
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
        return "doctor-admin-status-pending";
    }

    private String buildLocation(DoctorAdminEntry doctor) {
        String address = safeText(doctor.getDoctorAddress(), "");
        if (!address.isBlank()) {
            return address;
        }
        return safeText(doctor.getAddress(), "-");
    }

    private String experienceText(DoctorAdminEntry doctor) {
        Integer experience = doctor.getExperience();
        return experience == null ? "Expérience: -" : experience + " ans d'expérience";
    }

    private String formatPrice(Double value) {
        if (value == null) {
            return "-";
        }
        return PRICE_FORMAT.format(value);
    }

    private String initialsOf(String value) {
        if (value == null || value.isBlank()) {
            return "DM";
        }
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return safeText(value, "-");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int sortStatusRank(DoctorAdminEntry doctor) {
        return switch (doctor.getAccountStatus()) {
            case PENDING -> 0;
            case VERIFIED -> 1;
            case REJECTED -> 2;
            case SUSPENDED -> 3;
        };
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
