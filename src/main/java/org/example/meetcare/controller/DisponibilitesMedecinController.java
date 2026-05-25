package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.DoctorWorkspaceDao;
import org.example.meetcare.model.AvailabilitySlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DisponibilitesMedecinController {
    @FXML private DatePicker availabilityDatePicker;
    @FXML private VBox slotsContainer;
    @FXML private Label availabilityCountLabel;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        availabilityDatePicker.setValue(LocalDate.now());
        availabilityDatePicker.setOnAction(event -> refreshSlots());
        refreshSlots();
    }

    @FXML
    private void handleAddSlot() {
        LocalDate selectedDate = getSelectedDate();
        Optional<LocalTime> startTime = askTime("Heure de debut (HH:mm)", "09:00");
        if (startTime.isEmpty()) {
            return;
        }
        Optional<LocalTime> endTime = askTime("Heure de fin (HH:mm)", "09:30");
        if (endTime.isEmpty()) {
            return;
        }
        if (!endTime.get().isAfter(startTime.get())) {
            showInfo("Horaire invalide", "L'heure de fin doit etre apres l'heure de debut.");
            return;
        }
        doctorWorkspaceDao.addAvailabilitySlot(selectedDate, startTime.get(), endTime.get(), "DISPONIBLE", "Creneau ouvert");
        refreshSlots();
    }

    @FXML
    private void handleBlockDay() {
        doctorWorkspaceDao.replaceDayWithUnavailability(getSelectedDate());
        refreshSlots();
    }

    @FXML
    private void handleClearDay() {
        doctorWorkspaceDao.clearDayUnavailability(getSelectedDate());
        refreshSlots();
    }

    private void refreshSlots() {
        List<AvailabilitySlot> slots = doctorWorkspaceDao.getAvailabilitySlots(getSelectedDate());
        availabilityCountLabel.setText(slots.size() + (slots.size() > 1 ? " creneaux" : " creneau"));
        slotsContainer.getChildren().clear();

        if (slots.isEmpty()) {
            VBox empty = new VBox(6);
            empty.getStyleClass().add("doctor-card");
            Label title = new Label("Aucun creneau pour cette date");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label subtitle = new Label("Ajoutez vos disponibilites ou bloquez votre journee.");
            subtitle.getStyleClass().add("doctor-subtitle");
            empty.getChildren().addAll(title, subtitle);
            slotsContainer.getChildren().add(empty);
            return;
        }

        for (AvailabilitySlot slot : slots) {
            HBox row = new HBox(12);
            row.getStyleClass().add("doctor-list-row");

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label time = new Label(slot.toShortDisplayLabel());
            time.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label subtitle = new Label(slot.isBooked() ? "Reserve pour " + slot.getPatientName() : (slot.getNotes().isBlank() ? "Libre" : slot.getNotes()));
            subtitle.getStyleClass().add("doctor-subtitle");
            info.getChildren().addAll(time, subtitle);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(slot.isBooked() ? "Reserve" : (slot.getStatus().equalsIgnoreCase("DISPONIBLE") ? "Disponible" : "Indisponible"));
            status.getStyleClass().add(slot.isBooked()
                    ? "doctor-badge-orange"
                    : (slot.getStatus().equalsIgnoreCase("DISPONIBLE") ? "doctor-badge-green" : "doctor-badge-neutral"));

            row.getChildren().addAll(info, spacer, status);
            slotsContainer.getChildren().add(row);
        }
    }

    private Optional<LocalTime> askTime(String prompt, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle("Disponibilites");
        dialog.setHeaderText("Gestion du " + getSelectedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dialog.setContentText(prompt);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalTime.parse(result.get().trim()));
        } catch (Exception ignored) {
            showInfo("Horaire invalide", "Utilise le format HH:mm, par exemple 09:00.");
            return Optional.empty();
        }
    }

    private LocalDate getSelectedDate() {
        return availabilityDatePicker.getValue() == null ? LocalDate.now() : availabilityDatePicker.getValue();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
