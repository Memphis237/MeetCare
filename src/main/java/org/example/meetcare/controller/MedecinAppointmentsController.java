package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.meetcare.dao.AppointmentDao;
import org.example.meetcare.dao.AvailabilityBlockDao;
import org.example.meetcare.model.Appointment;
import org.example.meetcare.model.AvailabilityBlock;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MedecinAppointmentsController {
    private static final DateTimeFormatter RANGE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> TIME_OPTIONS = buildTimeOptions();

    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final AvailabilityBlockDao availabilityBlockDao = new AvailabilityBlockDao();

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label rangeLabel;

    @FXML
    private Label weekAppointmentsCountLabel;

    @FXML
    private Label weekAppointmentsMetaLabel;

    @FXML
    private Label confirmedCountLabel;

    @FXML
    private Label confirmedMetaLabel;

    @FXML
    private Label completedCountLabel;

    @FXML
    private Label completedMetaLabel;

    @FXML
    private Label teleconsultationsCountLabel;

    @FXML
    private Label teleconsultationsMetaLabel;

    @FXML
    private VBox appointmentsContainer;

    @FXML
    private Button todayFilterButton;

    @FXML
    private Button allFilterButton;

    @FXML
    private Button confirmedFilterButton;

    @FXML
    private Button completedFilterButton;

    @FXML
    private Button cancelledFilterButton;

    @FXML
    private Button teleconsultationFilterButton;

    @FXML
    private Button manageAvailabilityButton;

    private String activeFilter = "ALL";
    private List<Appointment> weekAppointments = List.of();

    @FXML
    private void initialize() {
        applyCounterStyles();
        refreshView();
        updateFilterStyles();
    }

    private void applyCounterStyles() {
        String style = "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #0f172a;";
        weekAppointmentsCountLabel.setStyle(style);
        confirmedCountLabel.setStyle(style + " -fx-text-fill: #16a34a;");
        completedCountLabel.setStyle(style + " -fx-text-fill: #2563eb;");
        teleconsultationsCountLabel.setStyle(style + " -fx-text-fill: #7c3aed;");
    }

    @FXML
    private void handleTodayFilter(ActionEvent event) {
        activeFilter = "TODAY";
        updateFilterStyles();
        renderAppointments();
    }

    @FXML
    private void handleAllFilter(ActionEvent event) {
        activeFilter = "ALL";
        updateFilterStyles();
        renderAppointments();
    }

    @FXML
    private void handleConfirmedFilter(ActionEvent event) {
        activeFilter = "CONFIRME";
        updateFilterStyles();
        renderAppointments();
    }

    @FXML
    private void handleCompletedFilter(ActionEvent event) {
        activeFilter = "TERMINE";
        updateFilterStyles();
        renderAppointments();
    }

    @FXML
    private void handleCancelledFilter(ActionEvent event) {
        activeFilter = "ANNULE";
        updateFilterStyles();
        renderAppointments();
    }

    @FXML
    private void handleTeleconsultationFilter(ActionEvent event) {
        activeFilter = "TELECONSULTATION";
        updateFilterStyles();
        renderAppointments();
    }

    @FXML
    private void handleManageAvailability(ActionEvent event) {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUsers() <= 0) {
            showAlert(AlertType.WARNING, "Session invalide", "Aucun médecin connecté.");
            return;
        }

        Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Gestion disponibilité");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.getStylesheets().add(getClass().getResource("/css/doctor-pages.css").toExternalForm());
        dialogPane.setPrefWidth(1120);
        dialogPane.setPrefHeight(720);

        VBox content = new VBox(18);
        content.setStyle("-fx-padding: 24; -fx-background-color: #f8fbff;");

        VBox formCard = new VBox(14);
        formCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 22; -fx-border-color: #e5e7eb; -fx-border-radius: 22; -fx-padding: 22;");
        formCard.setMaxWidth(Double.MAX_VALUE);

        VBox formHeader = new VBox(8);
        Label title = new Label("Gestion des disponibilités");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label subtitle = new Label(
                "Bloque une journée entière ou un créneau précis. Les patients verront ces plages comme indisponibles.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        formHeader.getChildren().addAll(title, subtitle, new Separator());

        VBox form = new VBox(16);
        form.setMaxWidth(Double.MAX_VALUE);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("doctor-input");
        datePicker.setPrefWidth(180);
        datePicker.setMaxWidth(Double.MAX_VALUE);

        CheckBox wholeDayCheck = new CheckBox("Journée complète");
        wholeDayCheck.setSelected(true);
        wholeDayCheck.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        ComboBox<String> startTimeBox = new ComboBox<>(FXCollections.observableArrayList(TIME_OPTIONS));
        startTimeBox.setPromptText("Début");
        startTimeBox.getStyleClass().add("doctor-input");
        startTimeBox.setPrefWidth(140);
        startTimeBox.setMaxWidth(Double.MAX_VALUE);
        startTimeBox.setDisable(true);

        ComboBox<String> endTimeBox = new ComboBox<>(FXCollections.observableArrayList(TIME_OPTIONS));
        endTimeBox.setPromptText("Fin");
        endTimeBox.getStyleClass().add("doctor-input");
        endTimeBox.setPrefWidth(140);
        endTimeBox.setMaxWidth(Double.MAX_VALUE);
        endTimeBox.setDisable(true);

        TextField reasonField = new TextField();
        reasonField.setPromptText("Ex: rendez-vous perso, congé, intervention...");
        reasonField.getStyleClass().add("doctor-input");
        reasonField.setMaxWidth(Double.MAX_VALUE);

        Label fullDayHint = new Label("La journée entière sera bloquée.");
        fullDayHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        fullDayHint.setWrapText(true);

        wholeDayCheck.selectedProperty().addListener((obs, oldValue, isSelected) -> {
            boolean timed = !isSelected;
            startTimeBox.setDisable(!timed);
            endTimeBox.setDisable(!timed);
            fullDayHint.setText(isSelected
                    ? "La journée entière sera bloquée."
                    : "Choisissez un créneau précis entre deux heures.");
            if (!timed) {
                startTimeBox.setValue(null);
                endTimeBox.setValue(null);
            }
        });

        Label dateLabel = new Label("Date");
        dateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");
        Label typeLabel = new Label("Type");
        typeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");
        Label startLabel = new Label("Début");
        startLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");
        Label endLabel = new Label("Fin");
        endLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");
        Label reasonLabel = new Label("Motif");
        reasonLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        VBox dateField = new VBox(8, dateLabel, datePicker);
        VBox typeField = new VBox(8, typeLabel, wholeDayCheck);
        VBox startField = new VBox(8, startLabel, startTimeBox);
        VBox endField = new VBox(8, endLabel, endTimeBox);
        VBox reasonFieldBox = new VBox(8, reasonLabel, reasonField, fullDayHint);
        dateField.setFillWidth(true);
        typeField.setFillWidth(true);
        startField.setFillWidth(true);
        endField.setFillWidth(true);
        reasonFieldBox.setFillWidth(true);

        form.getChildren().addAll(dateField, typeField, startField, endField, reasonFieldBox);

        Button addBlockButton = new Button("Bloquer");
        addBlockButton.getStyleClass().add("doctor-primary-button");
        addBlockButton
                .setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 10 20;");

        VBox blocksCard = new VBox(12);
        blocksCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 22; -fx-border-color: #e5e7eb; -fx-border-radius: 22; -fx-padding: 22;");
        blocksCard.setMaxWidth(Double.MAX_VALUE);
        Label blocksTitle = new Label("Blocages existants");
        blocksTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label blocksSubtitle = new Label("Supprime un blocage pour rouvrir automatiquement les créneaux.");
        blocksSubtitle.setWrapText(true);
        blocksSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        VBox blocksContainer = new VBox(10);
        blocksContainer.setFillWidth(true);
        blocksContainer.setMaxWidth(Double.MAX_VALUE);
        refreshAvailabilityBlocks(blocksContainer, currentUser.getIdUsers());
        blocksCard.getChildren().addAll(blocksTitle, blocksSubtitle, blocksContainer);

        addBlockButton.setOnAction(addEvent -> {
            LocalDate selectedDate = datePicker.getValue();
            if (selectedDate == null) {
                showAlert(AlertType.WARNING, "Date manquante", "Veuillez sélectionner une date.");
                return;
            }

            boolean wholeDay = wholeDayCheck.isSelected();
            String startTime = startTimeBox.getValue();
            String endTime = endTimeBox.getValue();

            if (!wholeDay) {
                if (startTime == null || endTime == null) {
                    showAlert(AlertType.WARNING, "Créneau incomplet", "Veuillez choisir une heure de début et de fin.");
                    return;
                }

                LocalTime start = LocalTime.parse(startTime, TIME_FORMATTER);
                LocalTime end = LocalTime.parse(endTime, TIME_FORMATTER);
                if (!start.isBefore(end)) {
                    showAlert(AlertType.WARNING, "Créneau invalide",
                            "L'heure de fin doit être après l'heure de début.");
                    return;
                }
                if (hasConflict(currentUser.getIdUsers(), selectedDate, start, end)) {
                    showAlert(AlertType.INFORMATION, "Blocage existant",
                            "Un blocage existe déjà pour ce jour ou ce créneau.");
                    return;
                }

                availabilityBlockDao.addBlock(currentUser.getIdUsers(), selectedDate, startTime, endTime, false,
                        reasonField.getText());
            } else {
                if (availabilityBlockDao.isWholeDayBlocked(currentUser.getIdUsers(), selectedDate)) {
                    showAlert(AlertType.INFORMATION, "Blocage existant",
                            "Une indisponibilité à la journée existe déjà pour cette date.");
                    return;
                }

                availabilityBlockDao.addBlock(currentUser.getIdUsers(), selectedDate, null, null, true,
                        reasonField.getText());
            }

            reasonField.clear();
            refreshAvailabilityBlocks(blocksContainer, currentUser.getIdUsers());
            showAlert(AlertType.INFORMATION, "Blocage ajouté", "La disponibilité a bien été mise à jour.");
        });

        HBox cardsRow = new HBox(16, formCard, blocksCard);
        cardsRow.setFillHeight(true);
        HBox.setHgrow(formCard, Priority.ALWAYS);
        HBox.setHgrow(blocksCard, Priority.ALWAYS);
        formCard.setPrefWidth(640);
        blocksCard.setPrefWidth(400);

        HBox buttonRow = new HBox(addBlockButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        formCard.getChildren().addAll(formHeader, form, buttonRow);
        content.getChildren().add(cardsRow);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dialogPane.setContent(scrollPane);
        dialog.showAndWait();
    }

    private void refreshView() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUsers() <= 0) {
            weekAppointments = List.of();
            updateStats();
            renderAppointments();
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(6);

        subtitleLabel.setText("Agenda du " + today.format(RANGE_FORMATTER) + " au " + weekEnd.format(RANGE_FORMATTER));
        rangeLabel.setText("Du " + today.format(RANGE_FORMATTER) + " au " + weekEnd.format(RANGE_FORMATTER));

        weekAppointments = appointmentDao.findByDoctorUserId(currentUser.getIdUsers()).stream()
                .filter(appointment -> appointment.getDate() != null
                        && !appointment.getDate().isBefore(today)
                        && !appointment.getDate().isAfter(weekEnd))
                .sorted(Comparator.comparing(Appointment::getDate)
                        .thenComparing(MedecinAppointmentsController::appointmentTimeAsLocalTime))
                .collect(Collectors.toList());

        updateStats();
        renderAppointments();
    }

    private void updateStats() {
        long confirmed = countByStatus("CONFIRME");
        long completed = countByStatus("TERMINE");
        long teleconsultations = weekAppointments.stream().filter(Appointment::isTeleconsultation).count();

        weekAppointmentsCountLabel.setText(String.valueOf(weekAppointments.size()));
        weekAppointmentsMetaLabel
                .setText(weekAppointments.isEmpty() ? "Aucun rendez-vous cette semaine" : "Consultations planifiées");
        confirmedCountLabel.setText(String.valueOf(confirmed));
        confirmedMetaLabel.setText("Consultations valides");
        completedCountLabel.setText(String.valueOf(completed));
        completedMetaLabel.setText("Rendez-vous clôturés");
        teleconsultationsCountLabel.setText(String.valueOf(teleconsultations));
        teleconsultationsMetaLabel.setText("À distance");
        updateFilterStyles();
    }

    private void renderAppointments() {
        updateFilterStyles();
        appointmentsContainer.getChildren().clear();

        List<Appointment> filtered = filterAppointments();
        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-padding: 18;");
            Label title = new Label("Aucun rendez-vous pour ce filtre");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            Label subtitle = new Label("Change de filtre pour voir les autres rendez-vous de la semaine.");
            subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            emptyState.getChildren().addAll(title, subtitle);
            appointmentsContainer.getChildren().add(emptyState);
            return;
        }

        for (Appointment appointment : filtered) {
            appointmentsContainer.getChildren().add(createAppointmentCard(appointment));
        }
    }

    private void refreshAvailabilityBlocks(VBox container, int doctorUserId) {
        container.getChildren().clear();
        List<AvailabilityBlock> blocks = availabilityBlockDao.findByDoctorUserId(doctorUserId);

        if (blocks.isEmpty()) {
            VBox emptyState = new VBox(6);
            emptyState.setStyle(
                    "-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-border-color: #dbe5f0; -fx-border-radius: 14; -fx-padding: 16;");
            Label emptyTitle = new Label("Aucun blocage pour le moment");
            emptyTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            Label emptySubtitle = new Label("Ajoutez une indisponibilité pour masquer un jour ou un créneau.");
            emptySubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            emptySubtitle.setWrapText(true);
            emptyState.getChildren().addAll(emptyTitle, emptySubtitle);
            container.getChildren().add(emptyState);
            return;
        }

        for (AvailabilityBlock block : blocks) {
            container.getChildren().add(createAvailabilityBlockCard(block, doctorUserId, container));
        }
    }

    private HBox createAppointmentCard(Appointment appointment) {
        HBox card = new HBox(16);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-padding: 16;");

        VBox slot = new VBox(4);
        slot.setMinWidth(86);
        Label day = new Label(formatShortDate(appointment.getDate()));
        day.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label time = new Label(formatTime(appointment.getTime()));
        time.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        slot.getChildren().addAll(day, time);

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label patient = new Label(safeText(appointment.getPatientName()));
        patient.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label speciality = new Label(safeText(appointment.getSpeciality()));
        speciality.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        Label details = new Label(safeText(appointment.getDetails()));
        details.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        details.setWrapText(true);
        info.getChildren().addAll(patient, speciality, details);

        VBox right = new VBox(8);
        right.setMinWidth(190);
        Label status = new Label(getDisplayStatus(appointment.getStatus()));
        status.setStyle(getStatusStyle(appointment.getStatus()));
        Label location = new Label(
                appointment.isTeleconsultation() ? "Téléconsultation" : safeText(appointment.getAddress()));
        location.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        location.setWrapText(true);
        Button detailsButton = new Button("Voir détails");
        detailsButton.setStyle(
                "-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-background-radius: 10; -fx-border-color: #bfdbfe; -fx-border-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 14;");
        detailsButton.setOnAction(event -> showAppointmentDetails(appointment));
        right.getChildren().addAll(status, location, detailsButton);

        card.getChildren().addAll(slot, info, right);
        return card;
    }

    private HBox createAvailabilityBlockCard(AvailabilityBlock block, int doctorUserId, VBox container) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-border-color: #dbe5f0; -fx-border-radius: 14; -fx-padding: 14;");

        VBox left = new VBox(4);
        left.setMinWidth(180);
        Label dateLabel = new Label(formatDate(block.getBlockDate()));
        dateLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label typeLabel = new Label(block.isWholeDay() ? "Journée entière"
                : formatBlockTimeRange(block.getStartTime(), block.getEndTime()));
        typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2563eb; -fx-font-weight: 700;");
        left.getChildren().addAll(dateLabel, typeLabel);

        VBox center = new VBox(4);
        HBox.setHgrow(center, Priority.ALWAYS);
        Label reason = new Label(safeText(block.getReason()));
        reason.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        reason.setWrapText(true);
        Label meta = new Label(
                block.isWholeDay() ? "Blocage sur toute la journée" : "Blocage sur un créneau spécifique");
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        center.getChildren().addAll(reason, meta);

        Button removeButton = new Button("Retirer");
        removeButton.setStyle(
                "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 14;");
        removeButton.setOnAction(event -> {
            Alert confirm = new Alert(AlertType.CONFIRMATION);
            confirm.setTitle("Retirer le blocage");
            confirm.setHeaderText(null);
            confirm.setContentText("Voulez-vous supprimer ce blocage ?");
            confirm.showAndWait().ifPresent(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    availabilityBlockDao.deleteById(block.getId(), doctorUserId);
                    refreshAvailabilityBlocks(container, doctorUserId);
                }
            });
        });

        card.getChildren().addAll(left, center, removeButton);
        return card;
    }

    private void showAppointmentDetails(Appointment appointment) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du rendez-vous");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 18; -fx-background-color: white;");

        Label title = new Label(safeText(appointment.getPatientName()));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lines = new Label(
                "Date : " + formatDate(appointment.getDate()) + "\n" +
                        "Heure : " + formatTime(appointment.getTime()) + "\n" +
                        "Statut : " + getDisplayStatus(appointment.getStatus()) + "\n" +
                        "Type : " + (appointment.isTeleconsultation() ? "Téléconsultation" : "Cabinet") + "\n" +
                        "Motif : " + safeText(appointment.getDetails()) + "\n" +
                        "Lieu : " + safeText(appointment.getAddress()));
        lines.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 4;");
        lines.setWrapText(true);

        content.getChildren().addAll(title, lines);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private boolean hasConflict(int doctorUserId, LocalDate date, LocalTime start, LocalTime end) {
        for (AvailabilityBlock block : availabilityBlockDao.findByDoctorUserId(doctorUserId)) {
            if (block.getBlockDate() == null || !block.getBlockDate().equals(date)) {
                continue;
            }
            if (block.isWholeDay()) {
                return true;
            }
            if (block.getStartTime() == null || block.getEndTime() == null) {
                continue;
            }

            LocalTime existingStart = LocalTime.parse(block.getStartTime(), TIME_FORMATTER);
            LocalTime existingEnd = LocalTime.parse(block.getEndTime(), TIME_FORMATTER);
            if (start.isBefore(existingEnd) && end.isAfter(existingStart)) {
                return true;
            }
        }
        return false;
    }

    private List<Appointment> filterAppointments() {
        LocalDate today = LocalDate.now();
        return weekAppointments.stream()
                .filter(appointment -> switch (activeFilter) {
                    case "TODAY" -> appointment.getDate() != null && appointment.getDate().isEqual(today);
                    case "CONFIRME", "TERMINE", "ANNULE" -> activeFilter.equalsIgnoreCase(safeStatus(appointment));
                    case "TELECONSULTATION" -> appointment.isTeleconsultation();
                    default -> true;
                })
                .collect(Collectors.toList());
    }

    private long countByStatus(String status) {
        return weekAppointments.stream().filter(appointment -> status.equalsIgnoreCase(safeStatus(appointment)))
                .count();
    }

    private void updateFilterStyles() {
        applyFilterStyle(allFilterButton, "ALL");
        applyFilterStyle(todayFilterButton, "TODAY");
        applyFilterStyle(confirmedFilterButton, "CONFIRME");
        applyFilterStyle(completedFilterButton, "TERMINE");
        applyFilterStyle(cancelledFilterButton, "ANNULE");
        applyFilterStyle(teleconsultationFilterButton, "TELECONSULTATION");
    }

    private void applyFilterStyle(Button button, String filter) {
        if (button == null) {
            return;
        }
        boolean active = filter.equals(activeFilter);
        button.setStyle(active
                ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-background-radius: 999; -fx-border-color: #bfdbfe; -fx-border-radius: 999; -fx-padding: 8 14; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-background-color: white; -fx-text-fill: #334155; -fx-background-radius: 999; -fx-border-color: #d1d5db; -fx-border-radius: 999; -fx-padding: 8 14; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    private static LocalTime appointmentTimeAsLocalTime(Appointment appointment) {
        try {
            return appointment == null || appointment.getTime() == null ? LocalTime.MIN
                    : LocalTime.parse(appointment.getTime());
        } catch (Exception ex) {
            return LocalTime.MIN;
        }
    }

    private String safeStatus(Appointment appointment) {
        return appointment == null || appointment.getStatus() == null ? "" : appointment.getStatus().trim();
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "-" : date.format(RANGE_FORMATTER);
    }

    private String formatTime(String time) {
        return time == null || time.isBlank() ? "-" : time;
    }

    private String formatBlockTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            return "Créneau horaire";
        }
        return startTime.trim() + " - " + endTime.trim();
    }

    private String getStatusStyle(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #ffedd5; -fx-text-fill: #ea580c; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    private String getDisplayStatus(String status) {
        if ("ANNULE".equalsIgnoreCase(status)) {
            return "Annulé";
        }
        if ("TERMINE".equalsIgnoreCase(status)) {
            return "Terminé";
        }
        return "Confirmé";
    }

    private static List<String> buildTimeOptions() {
        List<String> options = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            options.add(String.format("%02d:00", hour));
            options.add(String.format("%02d:30", hour));
        }
        return options;
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
