package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;
import org.example.meetcare.model.AvailabilitySlot;
import org.example.meetcare.utils.NavigationUtil;
import org.example.meetcare.utils.SelectedPatientStore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AgendaMedecinController {
    @FXML private DatePicker agendaDatePicker;
    @FXML private Label selectedDateLabel;
    @FXML private VBox agendaContainer;
    @FXML private VBox availabilityContainer;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        agendaDatePicker.setValue(LocalDate.now());
        agendaDatePicker.setOnAction(event -> refreshAgenda());
        refreshAgenda();
    }

    @FXML
    private void handleAddAvailabilitySlot() {
        LocalDate selectedDate = getSelectedDate();

        TextInputDialog startDialog = new TextInputDialog("09:00");
        startDialog.setTitle("Ajouter un creneau");
        startDialog.setHeaderText("Creneau disponible du " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        startDialog.setContentText("Heure de debut (HH:mm) :");
        Optional<String> startResult = startDialog.showAndWait();
        if (startResult.isEmpty()) {
            return;
        }

        TextInputDialog endDialog = new TextInputDialog("09:30");
        endDialog.setTitle("Ajouter un creneau");
        endDialog.setHeaderText("Creneau disponible du " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        endDialog.setContentText("Heure de fin (HH:mm) :");
        Optional<String> endResult = endDialog.showAndWait();
        if (endResult.isEmpty()) {
            return;
        }

        try {
            LocalTime startTime = LocalTime.parse(startResult.get().trim());
            LocalTime endTime = LocalTime.parse(endResult.get().trim());
            if (!endTime.isAfter(startTime)) {
                showInfo("Horaire invalide", "L'heure de fin doit etre apres l'heure de debut.");
                return;
            }
            doctorWorkspaceDao.addAvailabilitySlot(selectedDate, startTime, endTime, "DISPONIBLE", "Creneau ouvert depuis l'agenda");
            refreshAgenda();
        } catch (Exception ignored) {
            showInfo("Horaire invalide", "Utilise le format HH:mm, par exemple 09:00.");
        }
    }

    @FXML
    private void handleBlockSelectedDay() {
        doctorWorkspaceDao.replaceDayWithUnavailability(getSelectedDate());
        refreshAgenda();
    }

    @FXML
    private void handleClearSelectedDay() {
        doctorWorkspaceDao.clearDayUnavailability(getSelectedDate());
        refreshAgenda();
    }

    private void refreshAgenda() {
        LocalDate selectedDate = getSelectedDate();
        selectedDateLabel.setText("Rendez-vous du " + selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")));

        List<DoctorWorkspaceDao.AppointmentRecord> appointments = doctorWorkspaceDao.getAppointments().stream()
                .filter(appointment -> appointment.date().equals(selectedDate))
                .toList();

        agendaContainer.getChildren().clear();
        if (appointments.isEmpty()) {
            agendaContainer.getChildren().add(buildEmptyCard(
                    "Aucun rendez-vous ce jour",
                    "Quand un patient reserve un de vos creneaux, il apparaitra ici."
            ));
        } else {
            for (DoctorWorkspaceDao.AppointmentRecord appointment : appointments) {
                agendaContainer.getChildren().add(createAppointmentRow(appointment));
            }
        }

        renderAvailability(selectedDate);
    }

    private void renderAvailability(LocalDate selectedDate) {
        availabilityContainer.getChildren().clear();
        List<AvailabilitySlot> slots = doctorWorkspaceDao.getAvailabilitySlots(selectedDate);

        if (slots.isEmpty()) {
            availabilityContainer.getChildren().add(buildEmptyCard(
                    "Aucun creneau defini",
                    "Ajoute des disponibilites pour que les patients puissent reserver."
            ));
            return;
        }

        for (AvailabilitySlot slot : slots) {
            HBox row = new HBox(12);
            row.getStyleClass().add("doctor-list-row");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label timeLabel = new Label(slot.toShortDisplayLabel());
            timeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            String subtitleText = slot.isBooked()
                    ? "Reserve pour " + slot.getPatientName()
                    : (slot.getNotes() == null || slot.getNotes().isBlank() ? "Libre" : slot.getNotes());
            Label subtitle = new Label(subtitleText);
            subtitle.getStyleClass().add("doctor-subtitle");
            info.getChildren().addAll(timeLabel, subtitle);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(resolveAvailabilityLabel(slot));
            status.getStyleClass().add(resolveAvailabilityStyle(slot));

            Button toggleButton = new Button(slot.getStatus().equalsIgnoreCase("DISPONIBLE") ? "Rendre indisponible" : "Rendre disponible");
            toggleButton.getStyleClass().add("doctor-mini-button");
            toggleButton.setDisable(slot.isBooked());
            toggleButton.setOnAction(event -> {
                doctorWorkspaceDao.updateAvailabilityStatus(slot.getId(),
                        slot.getStatus().equalsIgnoreCase("DISPONIBLE") ? "INDISPONIBLE" : "DISPONIBLE");
                refreshAgenda();
            });

            row.getChildren().addAll(info, spacer, status, toggleButton);
            availabilityContainer.getChildren().add(row);
        }
    }

    private HBox createAppointmentRow(DoctorWorkspaceDao.AppointmentRecord appointment) {
        HBox row = new HBox(16);
        row.getStyleClass().add("doctor-list-row");

        VBox timeBox = new VBox(4);
        timeBox.getStyleClass().add("doctor-time-box");
        Label time = new Label(appointment.time());
        time.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #9333ea;");
        Label duration = new Label(appointment.durationMinutes() + " min");
        duration.setStyle("-fx-font-size: 12px; -fx-text-fill: #9333ea;");
        timeBox.getChildren().addAll(time, duration);

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox titleRow = new HBox(10);
        Label patient = new Label(appointment.patientName());
        patient.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label status = new Label(appointment.status());
        status.getStyleClass().add("En attente".equals(appointment.status()) ? "doctor-badge-orange" : "doctor-badge-green");
        titleRow.getChildren().addAll(patient, status);
        Label purpose = new Label(appointment.purpose() + " - " + appointment.mode());
        purpose.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        info.getChildren().addAll(titleRow, purpose);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.getChildren().addAll(
                createActionButton("Voir", () -> openPatientDetail(appointment.patientId(), "history", row)),
                createActionButton("Doc", () -> openPatientDetail(appointment.patientId(), "documents", row)),
                createActionButton("Facture", () -> openPatientDetail(appointment.patientId(), "invoices", row)),
                createActionButton("Msg", this::showMessageInfo)
        );

        row.getChildren().addAll(timeBox, info, spacer, actions);
        return row;
    }

    private VBox buildEmptyCard(String titleText, String subtitleText) {
        VBox box = new VBox(6);
        box.getStyleClass().add("doctor-list-row");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("doctor-subtitle");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Button createActionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("doctor-mini-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void openPatientDetail(int patientId, String tab, HBox source) {
        SelectedPatientStore.setSelection(patientId, tab);
        NavigationUtil.navigateTo(source, "/view/patientDetailMedecin.fxml", 1200, 760, "MeetCare - Dossier patient");
    }

    private void showMessageInfo() {
        showInfo("Messagerie", "La messagerie detaillee sera disponible ici.");
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private LocalDate getSelectedDate() {
        return agendaDatePicker.getValue() == null ? LocalDate.now() : agendaDatePicker.getValue();
    }

    private String resolveAvailabilityLabel(AvailabilitySlot slot) {
        if (slot.isBooked()) {
            return "Reserve";
        }
        return slot.getStatus().equalsIgnoreCase("DISPONIBLE") ? "Disponible" : "Indisponible";
    }

    private String resolveAvailabilityStyle(AvailabilitySlot slot) {
        if (slot.isBooked()) {
            return "doctor-badge-orange";
        }
        return slot.getStatus().equalsIgnoreCase("DISPONIBLE") ? "doctor-badge-green" : "doctor-badge-neutral";
    }
}
