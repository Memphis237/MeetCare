package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.example.meetcare.exception.DatabaseException;
import org.example.meetcare.dao.DoctorDocumentDao;
import org.example.meetcare.model.AccountStatus;
import org.example.meetcare.model.DoctorDocumentEntry;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.stage.Stage;

public class ValidationMedecinController {
    @FXML
    private Label accountStatusLabel;

    @FXML
    private Label selectedFilesLabel;

    @FXML
    private ChoiceBox<String> documentTypeChoiceBox;

    @FXML
    private VBox documentsContainer;

    private final DoctorDocumentDao doctorDocumentDao = new DoctorDocumentDao();
    private final List<File> selectedFiles = new ArrayList<>();

    @FXML
    private void initialize() {
        if (documentTypeChoiceBox != null) {
            documentTypeChoiceBox.setItems(FXCollections.observableArrayList(
                    "RIB",
                    "CARTE_VITALE",
                    "TITRE_SEJOUR",
                    "PASSEPORT",
                    "VISA",
                    "JUSTIFICATIF_DOMICILE",
                    "ASSURANCE_HABITATION",
                    "PHOTO_PRO",
                    "DIPLOMA",
                    "LICENSE",
                    "ID_CARD"
            ));
            documentTypeChoiceBox.setValue("CARTE_VITALE");
        }

        refreshView();
    }

    @FXML
    private void handleChooseFiles(ActionEvent event) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choisir les fichiers de validation");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new javafx.stage.FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        List<File> files = chooser.showOpenMultipleDialog(getWindow(event));
        if (files == null || files.isEmpty()) {
            return;
        }

        selectedFiles.clear();
        selectedFiles.addAll(files);
        updateSelectedFilesLabel();
    }

    @FXML
    private void handleUpload(ActionEvent event) {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun utilisateur connecte.");
            return;
        }
        if (selectedFiles.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Aucun fichier", "Veuillez selectionner au moins un fichier.");
            return;
        }

        String documentType = documentTypeChoiceBox == null || documentTypeChoiceBox.getValue() == null
                ? "DIPLOMA"
                : documentTypeChoiceBox.getValue();

        try {
            for (File file : selectedFiles) {
                File storedFile = copyToDoctorFolder(currentUser.getIdUsers(), file);
                doctorDocumentDao.saveUploadedDocument(currentUser.getIdUsers(), storedFile, documentType);
            }
            selectedFiles.clear();
            updateSelectedFilesLabel();
            refreshView();
            showAlert(Alert.AlertType.INFORMATION, "Documents envoyes", "Vos fichiers ont ete envoyes pour validation.");
        } catch (DatabaseException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees",
                    "La table doctor_documents de votre base actuelle n'a probablement pas encore ete migree avec les nouveaux types de documents. "
                            + "Applique l'ALTER TABLE du schema puis recommence. Details: " + rootCauseMessage(ex));
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer les fichiers: " + ex.getMessage());
        }
    }

    private void refreshView() {
        Users currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (accountStatusLabel != null) {
            AccountStatus status = currentUser.getAccountStatus();
            accountStatusLabel.setText(status == null ? "PENDING" : status.name());
            accountStatusLabel.setStyle(statusStyle(status));
        }

        loadDocuments(currentUser.getIdUsers());
    }

    private void loadDocuments(int userId) {
        if (documentsContainer == null) {
            return;
        }

        documentsContainer.getChildren().clear();
        List<DoctorDocumentEntry> documents = doctorDocumentDao.findByDoctorUserId(userId);
        if (documents.isEmpty()) {
            Label empty = new Label("Aucun document envoye pour le moment.");
            empty.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
            documentsContainer.getChildren().add(empty);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (DoctorDocumentEntry document : documents) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 18; -fx-border-color: #E5E7EB; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(17,24,39,0.08), 10, 0, 0, 2);");

            HBox topRow = new HBox(12);
            topRow.setStyle("-fx-alignment: center-left;");

            Rectangle icon = new Rectangle(46, 46);
            icon.setArcHeight(14);
            icon.setArcWidth(14);
            icon.setStyle("-fx-fill: #EAF2FF;");

            VBox info = new VBox(4);
            Label title = new Label(document.getDocumentType() + " - " + document.getFileName());
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label meta = new Label("Envoye le " + (document.getUploadedAt() == null ? "-" : formatter.format(document.getUploadedAt())));
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

            Label path = new Label(document.getFilePath());
            path.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");
            path.setWrapText(true);

            info.getChildren().addAll(title, meta, path);

            Label status = new Label(document.getVerificationStatus());
            status.setStyle(documentStatusStyle(document.getVerificationStatus()));
            status.setMinWidth(96);
            status.setMaxWidth(96);
            status.setAlignment(javafx.geometry.Pos.CENTER);
            status.setPadding(new javafx.geometry.Insets(6, 10, 6, 10));

            HBox.setHgrow(info, Priority.ALWAYS);
            topRow.getChildren().addAll(icon, info, status);
            card.getChildren().add(topRow);
            documentsContainer.getChildren().add(card);
        }
    }

    private File copyToDoctorFolder(int userId, File source) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.dir"), "uploads", "doctor-documents", "user-" + userId);
        Files.createDirectories(baseDir);
        String safeName = System.currentTimeMillis() + "-" + source.getName();
        Path target = baseDir.resolve(safeName);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toFile();
    }

    private void updateSelectedFilesLabel() {
        if (selectedFilesLabel == null) {
            return;
        }
        if (selectedFiles.isEmpty()) {
            selectedFilesLabel.setText("Aucun fichier selectionne.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < selectedFiles.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(selectedFiles.get(i).getName());
        }
        selectedFilesLabel.setText(selectedFiles.size() + " fichier(s): " + builder);
    }

    private String statusStyle(AccountStatus status) {
        if (status == AccountStatus.VERIFIED) {
            return "-fx-background-color: #E8F8EF; -fx-text-fill: #15803D; -fx-background-radius: 999; -fx-font-weight: bold; -fx-padding: 6 12 6 12;";
        }
        if (status == AccountStatus.REJECTED) {
            return "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-background-radius: 999; -fx-font-weight: bold; -fx-padding: 6 12 6 12;";
        }
        if (status == AccountStatus.SUSPENDED) {
            return "-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-background-radius: 999; -fx-font-weight: bold; -fx-padding: 6 12 6 12;";
        }
        return "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-background-radius: 999; -fx-font-weight: bold; -fx-padding: 6 12 6 12;";
    }

    private String documentStatusStyle(String status) {
        if ("APPROVED".equalsIgnoreCase(status)) {
            return "-fx-background-color: #E8F8EF; -fx-text-fill: #15803D; -fx-background-radius: 999; -fx-font-weight: bold;";
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-background-radius: 999; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-background-radius: 999; -fx-font-weight: bold;";
    }

    private Stage getWindow(ActionEvent event) {
        return (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }
}
