package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.meetcare.dao.MedicalRecordDao;
import org.example.meetcare.model.MedicalRecord;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.MedicalRecordDocumentUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DocumentsPatientController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> DOCUMENT_TYPES = List.of(
            "Tous les types",
            "Ordonnance",
            "Radio",
            "Analyse",
            "Scanner",
            "Compte rendu",
            "Certificat",
            "Electrocardiogramme",
            "IRM",
            "Echographie",
            "Autre"
    );

    private final MedicalRecordDao medicalRecordDao = new MedicalRecordDao();
    private final MedicalRecordDocumentUtil documentUtil = new MedicalRecordDocumentUtil();
    private final List<MedicalRecord> allRecords = new ArrayList<>();

    @FXML
    private TextField titleFilterField;

    @FXML
    private ComboBox<String> typeFilterCombo;

    @FXML
    private VBox recordsContainer;

    @FXML
    private Button importButton;

    private Users currentUser;

    @FXML
    private void initialize() {
        currentUser = AppSession.getCurrentUser();
        if (typeFilterCombo != null) {
            typeFilterCombo.setItems(FXCollections.observableArrayList(DOCUMENT_TYPES));
            typeFilterCombo.setValue("Tous les types");
        }

        if (titleFilterField != null) {
            titleFilterField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (typeFilterCombo != null) {
            typeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        refreshRecords();
    }

    @FXML
    private void handleFilterChange(ActionEvent event) {
        applyFilters();
    }

    @FXML
    private void handleImportDocument(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun utilisateur connecte.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un document medical");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Textes", "*.txt", "*.csv", "*.log")
        );

        File selectedFile = chooser.showOpenDialog(getWindow(event));
        if (selectedFile == null) {
            return;
        }

        Optional<DocumentFormData> result = showImportDialog(selectedFile.getName(), getWindow(event));
        if (result.isEmpty()) {
            return;
        }

        DocumentFormData formData = result.get();
        if (formData.title().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Titre requis", "Le titre du document est obligatoire.");
            return;
        }

        try {
            Path savedPath = copyToMedicalRecordsFolder(currentUser.getIdUsers(), selectedFile);
            MedicalRecord record = new MedicalRecord();
            record.setTitle(formData.title());
            record.setDescription(formData.description());
            record.setDocumentType(normalizeTypeForStorage(formData.documentType()));
            record.setFilePath(savedPath.toString());
            record.setIdDoctors(null);
            record.setIdUsers(currentUser.getIdUsers());

            try {
                medicalRecordDao.insert(record);
            } catch (Exception insertEx) {
                Files.deleteIfExists(savedPath);
                throw insertEx;
            }

            refreshRecords();
            showAlert(Alert.AlertType.INFORMATION, "Document enregistre", "Le document a ete ajoute a vos dossiers.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
        }
    }

    private void refreshRecords() {
        if (currentUser == null) {
            currentUser = AppSession.getCurrentUser();
        }
        if (currentUser == null) {
            return;
        }

        try {
            allRecords.clear();
            allRecords.addAll(medicalRecordDao.findByUserId(currentUser.getIdUsers()));
            applyFilters();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement", rootCauseMessage(ex));
        }
    }

    private void applyFilters() {
        if (recordsContainer == null) {
            return;
        }

        String titleFilter = titleFilterField == null ? "" : normalizeText(titleFilterField.getText());
        String typeFilter = typeFilterCombo == null ? "Tous les types" : typeFilterCombo.getValue();

        List<MedicalRecord> filtered = allRecords.stream()
                .filter(record -> titleFilter.isBlank() || normalizeText(record.getTitle()).contains(titleFilter))
                .filter(record -> "Tous les types".equalsIgnoreCase(typeFilter)
                        || normalizeDisplayType(record.getDocumentType()).equalsIgnoreCase(typeFilter))
                .toList();

        renderRecords(filtered);
    }

    private void renderRecords(List<MedicalRecord> records) {
        recordsContainer.getChildren().clear();

        if (records.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 22; -fx-border-color: #e5e7eb; -fx-border-radius: 16;");

            Label title = new Label("Aucun document ne correspond a ce filtre");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label subtitle = new Label("Essaie un autre titre ou un autre type de document.");
            subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");
            subtitle.setWrapText(true);

            empty.getChildren().addAll(title, subtitle);
            recordsContainer.getChildren().add(empty);
            return;
        }

        for (MedicalRecord record : records) {
            recordsContainer.getChildren().add(createRecordCard(record));
        }
    }

    private HBox createRecordCard(MedicalRecord record) {
        HBox card = new HBox(16);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        VBox icon = new VBox();
        icon.setMinSize(58, 58);
        icon.setMaxSize(58, 58);
        icon.setAlignment(javafx.geometry.Pos.CENTER);
        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #2563eb;");
        icon.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 14;");
        icon.getChildren().add(iconLabel);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(emptyIfBlank(record.getTitle(), "Document medical"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label description = new Label(emptyIfBlank(record.getDescription(), "Aucune description."));
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");
        description.setWrapText(true);

        Label meta = new Label("Ajoute le " + formatDate(record.getDate()));
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        info.getChildren().addAll(title, description, meta);

        VBox actions = new VBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label typeBadge = new Label(normalizeDisplayType(record.getDocumentType()));
        typeBadge.setStyle(documentBadgeStyle(record.getDocumentType()));

        HBox buttons = new HBox(8);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttons.getChildren().addAll(
                documentUtil.createConsultButton(record),
                documentUtil.createDownloadButton(record)
        );

        actions.getChildren().addAll(typeBadge, buttons);
        card.getChildren().addAll(icon, info, actions);
        return card;
    }

    private Optional<DocumentFormData> showImportDialog(String fileName, Window owner) {
        Dialog<DocumentFormData> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un document");
        dialog.setHeaderText("Renseigne les informations du document pour " + fileName);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        TextField titleField = new TextField();
        titleField.setPromptText("Titre du document");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description du document");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(DOCUMENT_TYPES.stream()
                .filter(type -> !"Tous les types".equalsIgnoreCase(type))
                .toList()));
        typeBox.setValue("Ordonnance");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(10);
        form.setStyle("-fx-padding: 8 2 0 2;");
        form.getChildren().addAll(
                fieldBlock("Titre", titleField),
                fieldBlock("Description", descriptionArea),
                fieldBlock("Type", typeBox)
        );

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(form);
        ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButton) {
                return null;
            }
            return new DocumentFormData(
                    titleField.getText() == null ? "" : titleField.getText().trim(),
                    descriptionArea.getText() == null ? "" : descriptionArea.getText().trim(),
                    typeBox.getValue() == null ? "Autre" : typeBox.getValue().trim()
            );
        });

        return dialog.showAndWait();
    }

    private VBox fieldBlock(String labelText, Node field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        if (field instanceof TextInputControl textInputControl) {
            textInputControl.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #dbe5f0; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 12;");
        } else if (field instanceof ComboBox<?> comboBox) {
            comboBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #dbe5f0; -fx-background-radius: 10; -fx-border-radius: 10;");
        }

        VBox box = new VBox(6, label, field);
        box.setFillWidth(true);
        return box;
    }

    private Path copyToMedicalRecordsFolder(int userId, File source) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.dir"), "uploads", "medical-records", "user-" + userId);
        Files.createDirectories(baseDir);
        String safeName = System.currentTimeMillis() + "-" + source.getName();
        Path target = baseDir.resolve(safeName);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().normalize();
    }

    private String normalizeDisplayType(String value) {
        if (value == null || value.isBlank()) {
            return "Autre";
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ORDONNANCE" -> "Ordonnance";
            case "RADIO" -> "Radio";
            case "ANALYSE" -> "Analyse";
            case "SCANNER" -> "Scanner";
            case "COMPTE_RENDU" -> "Compte rendu";
            case "CERTIFICAT" -> "Certificat";
            case "ELECTROCARDIOGRAMME" -> "Electrocardiogramme";
            case "IRM" -> "IRM";
            case "ECHOGRAPHIE" -> "Echographie";
            default -> "Autre";
        };
    }

    private String normalizeTypeForStorage(String displayType) {
        if (displayType == null || displayType.isBlank()) {
            return "OTHER";
        }

        return switch (displayType.trim().toLowerCase(Locale.ROOT)) {
            case "ordonnance" -> "ORDONNANCE";
            case "radio" -> "RADIO";
            case "analyse" -> "ANALYSE";
            case "scanner" -> "SCANNER";
            case "compte rendu" -> "COMPTE_RENDU";
            case "certificat" -> "CERTIFICAT";
            case "electrocardiogramme" -> "ELECTROCARDIOGRAMME";
            case "irm" -> "IRM";
            case "echographie" -> "ECHOGRAPHIE";
            default -> "OTHER";
        };
    }

    private String documentBadgeStyle(String type) {
        String normalized = type == null ? "OTHER" : type.trim().toUpperCase(Locale.ROOT);
        if ("ORDONNANCE".equals(normalized)) {
            return "-fx-background-color: #eff6ff; -fx-text-fill: #2563eb; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-weight: bold;";
        }
        if ("RADIO".equals(normalized)) {
            return "-fx-background-color: #f3e8ff; -fx-text-fill: #9333ea; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-weight: bold;";
        }
        if ("ANALYSE".equals(normalized)) {
            return "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-weight: bold;";
        }
        if ("SCANNER".equals(normalized)) {
            return "-fx-background-color: #ffedd5; -fx-text-fill: #ea580c; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-weight: bold;";
        }
        if ("COMPTE_RENDU".equals(normalized)) {
            return "-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-weight: bold;";
        }
        if ("CERTIFICAT".equals(normalized)) {
            return "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 10; -fx-padding: 5 12; -fx-font-weight: bold;";
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return DATE_FORMATTER.format(dateTime);
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private String emptyIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Window getWindow(ActionEvent event) {
        return ((Node) event.getSource()).getScene().getWindow();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private record DocumentFormData(String title, String description, String documentType) {
    }
}
