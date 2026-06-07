package org.example.meetcare.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.model.Appointment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AppointmentsAdminController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);

    private final AppointmentDao appointmentDao = new AppointmentDao();

    @FXML
    private VBox contentContainer;

    private Button allFilterButton;
    private Button confirmedFilterButton;
    private Button urgentFilterButton;

    private Label confirmedCountLabel;
    private Label completedCountLabel;
    private Label cancelledCountLabel;
    private Label urgentCountLabel;

    private VBox rowsContainer;
    private List<Appointment> appointments = List.of();
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
        refreshRows();
        updateFilterStyles();
    }

    @FXML
    private void handleConfirmedFilter(ActionEvent event) {
        activeFilter = "CONFIRME";
        refreshRows();
        updateFilterStyles();
    }

    @FXML
    private void handleUrgentFilter(ActionEvent event) {
        activeFilter = "URGENT";
        refreshRows();
        updateFilterStyles();
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
                buildTableCard()
        );
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(6);
        Label title = new Label("Gestion des rendez-vous");
        title.getStyleClass().add("admin-page-title");

        Label subtitle = new Label("Vue d'ensemble de tous les rendez-vous de la plateforme");
        subtitle.getStyleClass().add("admin-page-subtitle");

        texts.getChildren().addAll(title, subtitle);
        HBox.setHgrow(texts, Priority.ALWAYS);

        header.getChildren().add(texts);
        return header;
    }

    private HBox buildStatsRow() {
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        statsRow.getChildren().addAll(
                createStatCard("Confirmés", "0", "admin-stat-card-confirmed", "admin-stat-title-confirmed", "admin-stat-value-confirmed", "admin-stat-icon-confirmed", "\u2713"),
                createStatCard("Terminés", "0", "admin-stat-card-completed", "admin-stat-title-completed", "admin-stat-value-completed", "admin-stat-icon-completed", "\u231B"),
                createStatCard("Annulés", "0", "admin-stat-card-cancelled", "admin-stat-title-cancelled", "admin-stat-value-cancelled", "admin-stat-icon-cancelled", "\u2715"),
                createStatCard("Urgents", "0", "admin-stat-card-urgent", "admin-stat-title-urgent", "admin-stat-value-urgent", "admin-stat-icon-urgent", "!")
        );

        return statsRow;
    }

    private VBox createStatCard(String titleText,
                                String initialValue,
                                String cardStyle,
                                String titleStyle,
                                String valueStyle,
                                String iconStyle,
                                String iconText) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("admin-stat-card", cardStyle);
        card.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconText);
        icon.getStyleClass().addAll("admin-stat-icon", iconStyle);

        Label title = new Label(titleText);
        title.getStyleClass().addAll("admin-stat-title", titleStyle);

        Label value = new Label(initialValue);
        value.getStyleClass().addAll("admin-stat-value", valueStyle);

        switch (titleText) {
            case "Confirmés" -> confirmedCountLabel = value;
            case "Terminés" -> completedCountLabel = value;
            case "Annulés" -> cancelledCountLabel = value;
            case "Urgents" -> urgentCountLabel = value;
            default -> {
            }
        }

        card.getChildren().addAll(icon, title, value);
        return card;
    }

    private VBox buildFilterCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("admin-filter-card");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label filterIcon = new Label("\u29C7");
        filterIcon.getStyleClass().add("admin-filter-icon");

        allFilterButton = createFilterButton("Tous", this::handleAllFilter);
        confirmedFilterButton = createFilterButton("Confirmés", this::handleConfirmedFilter);
        urgentFilterButton = createFilterButton("Urgents", this::handleUrgentFilter);

        row.getChildren().addAll(filterIcon, allFilterButton, confirmedFilterButton, urgentFilterButton);
        card.getChildren().add(row);
        return card;
    }

    private Button createFilterButton(String text, javafx.event.EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.getStyleClass().add("admin-filter-chip");
        button.setOnAction(action);
        return button;
    }

    private VBox buildTableCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("admin-table-card");

        HBox header = new HBox();
        header.getStyleClass().add("admin-table-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));

        header.getChildren().addAll(
                createHeaderCell("Date", 140),
                createHeaderCell("Heure", 90),
                createHeaderCell("Patient", 110),
                createHeaderCell("Médecin", 220),
                createHeaderCell("Motif", 280),
                createHeaderCell("Urgence", 120),
                createHeaderCell("Statut", 120)
        );

        rowsContainer = new VBox(0);
        card.getChildren().addAll(header, new Separator(), rowsContainer);
        return card;
    }

    private Label createHeaderCell(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-table-header-label");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        return label;
    }

    private Label createBodyCell(String text, double width, boolean wrap) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-table-cell");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setWrapText(wrap);
        return label;
    }

    private Label createSecondaryCell(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-table-cell-secondary");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        return label;
    }

    private Label createPill(String text, String style) {
        Label pill = new Label(text);
        pill.getStyleClass().add("admin-pill");
        pill.setStyle(style);
        return pill;
    }

    private void refreshData() {
        appointments = appointmentDao.findAllAppointments().stream()
                .sorted(Comparator.comparing(Appointment::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Appointment::getTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        updateStats();
        refreshRows();
        updateFilterStyles();
    }

    private void updateStats() {
        confirmedCountLabel.setText(String.valueOf(countByStatus("CONFIRME")));
        completedCountLabel.setText(String.valueOf(countByStatus("TERMINE")));
        cancelledCountLabel.setText(String.valueOf(countByStatus("ANNULE")));
        urgentCountLabel.setText(String.valueOf(countByUrgency("URGENT")));
    }

    private void refreshRows() {
        rowsContainer.getChildren().clear();
        List<Appointment> filtered = filterAppointments();

        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.getStyleClass().add("admin-table-empty");

            Label title = new Label("Aucun rendez-vous pour ce filtre");
            title.getStyleClass().add("admin-empty-title");

            Label subtitle = new Label("Essaye un autre filtre pour voir plus de rendez-vous.");
            subtitle.getStyleClass().add("admin-empty-subtitle");

            emptyState.getChildren().addAll(title, subtitle);
            rowsContainer.getChildren().add(emptyState);
            return;
        }

        for (Appointment appointment : filtered) {
            rowsContainer.getChildren().add(createRow(appointment));
        }
    }

    private HBox createRow(Appointment appointment) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openEditDialog(appointment);
            }
        });

        VBox dateBox = buildCellBox(
                createBodyCell(formatDate(appointment.getDate()), 140, false),
                createSecondaryCell(appointment.getTime(), 140)
        );

        VBox timeBox = buildCellBox(
                createBodyCell(formatTime(appointment.getTime()), 90, false),
                createSecondaryCell(appointment.isTeleconsultation() ? "Téléconsultation" : "Cabinet", 90)
        );

        Label patient = createBodyCell(safeText(appointment.getPatientName()), 110, false);
        Label doctor = createBodyCell(safeText(appointment.getDoctorName()), 220, false);
        Label motif = createBodyCell(safeText(appointment.getDetails()), 280, true);

        Label urgency = createPill(
                displayUrgency(appointment.getUrgency()),
                urgencyStyle(appointment.getUrgency())
        );
        urgency.setMinWidth(120);

        Label status = createPill(
                displayStatus(appointment.getStatus()),
                statusStyle(appointment.getStatus())
        );
        status.setMinWidth(120);

        HBox cellGroup = new HBox(0, dateBox, timeBox, patient, doctor, motif, urgency, status);
        HBox.setHgrow(motif, Priority.ALWAYS);
        HBox.setHgrow(cellGroup, Priority.ALWAYS);
        row.getChildren().add(cellGroup);
        return row;
    }

    private VBox buildCellBox(Label primary, Label secondary) {
        VBox box = new VBox(2);
        box.getChildren().addAll(primary, secondary);
        return box;
    }

    private void openEditDialog(Appointment appointment) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le rendez-vous");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(
                new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 18; -fx-background-color: white;");

        Label title = new Label(safeText(appointment.getPatientName()));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label subtitle = new Label(
                formatDate(appointment.getDate()) + " - " + formatTime(appointment.getTime()) + " - " + safeText(appointment.getDoctorName())
        );
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("CONFIRME", "TERMINE", "ANNULE");
        statusBox.setValue(safeStatus(appointment));
        statusBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> urgencyBox = new ComboBox<>();
        urgencyBox.getItems().addAll("NORMAL", "URGENT", "ELEVEE", "CRITIQUE");
        urgencyBox.setValue(safeUrgency(appointment));
        urgencyBox.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(
                title,
                subtitle,
                buildFormField("Statut", statusBox),
                buildFormField("Urgence", urgencyBox)
        );
        pane.setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if ("Enregistrer".equals(result.getText())) {
                appointmentDao.updateStatusAndUrgency(appointment.getId(), statusBox.getValue(), urgencyBox.getValue());
                refreshData();
            }
        });
    }

    private VBox buildFormField(String labelText, Node control) {
        VBox field = new VBox(6);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        field.getChildren().addAll(label, control);
        return field;
    }

    private List<Appointment> filterAppointments() {
        return appointments.stream()
                .filter(appointment -> switch (activeFilter) {
                    case "CONFIRME" -> "CONFIRME".equalsIgnoreCase(safeStatus(appointment));
                    case "URGENT" -> "URGENT".equalsIgnoreCase(safeUrgency(appointment));
                    default -> true;
                })
                .toList();
    }

    private long countByStatus(String status) {
        return appointments.stream()
                .filter(appointment -> status.equalsIgnoreCase(safeStatus(appointment)))
                .count();
    }

    private long countByUrgency(String urgency) {
        return appointments.stream()
                .filter(appointment -> urgency.equalsIgnoreCase(safeUrgency(appointment)))
                .count();
    }

    private void updateFilterStyles() {
        applyFilterStyle(allFilterButton, "ALL");
        applyFilterStyle(confirmedFilterButton, "CONFIRME");
        applyFilterStyle(urgentFilterButton, "URGENT");
    }

    private void applyFilterStyle(Button button, String filter) {
        if (button == null) {
            return;
        }

        boolean active = filter.equals(activeFilter);
        button.setStyle(active
                ? "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 14; -fx-border-radius: 14; -fx-padding: 10 16; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-background-color: #f3f4f6; -fx-text-fill: #334155; -fx-background-radius: 14; -fx-border-radius: 14; -fx-padding: 10 16; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private String statusStyle(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb;";
        }
        return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;";
    }

    private String urgencyStyle(String urgency) {
        if ("URGENT".equalsIgnoreCase(urgency) || "CRITIQUE".equalsIgnoreCase(urgency)) {
            return "-fx-background-color: #fff7ed; -fx-text-fill: #ea580c;";
        }
        if ("ELEVEE".equalsIgnoreCase(urgency)) {
            return "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;";
        }
        return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb;";
    }

    private String displayStatus(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "Annulé";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "Terminé";
        }
        return "Confirmé";
    }

    private String displayUrgency(String urgency) {
        if ("URGENT".equalsIgnoreCase(urgency)) {
            return "Urgent";
        }
        if ("CRITIQUE".equalsIgnoreCase(urgency)) {
            return "Critique";
        }
        if ("ELEVEE".equalsIgnoreCase(urgency)) {
            return "Élevée";
        }
        return "Normal";
    }

    private String safeStatus(Appointment appointment) {
        return appointment == null || appointment.getStatus() == null ? "" : appointment.getStatus().trim();
    }

    private String safeUrgency(Appointment appointment) {
        return appointment == null || appointment.getUrgency() == null ? "NORMAL" : appointment.getUrgency().trim();
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
}
