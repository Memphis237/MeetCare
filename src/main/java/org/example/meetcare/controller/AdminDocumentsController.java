package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.meetcare.dao.AdminWorkspaceDao;
import org.example.meetcare.utils.AdminNavigationState;

import java.util.List;
import java.util.Locale;

public class AdminDocumentsController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> patientFilterComboBox;
    @FXML private Label documentsCountLabel;
    @FXML private VBox documentsContainer;

    private final AdminWorkspaceDao adminWorkspaceDao = new AdminWorkspaceDao();

    @FXML
    private void initialize() {
        AdminNavigationState.setCurrentPage("documents");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshDocuments());
        typeFilterComboBox.setOnAction(event -> refreshDocuments());
        patientFilterComboBox.setOnAction(event -> refreshDocuments());
        reloadFilters();
        refreshDocuments();
    }

    private void reloadFilters() {
        List<AdminWorkspaceDao.AdminDocumentRecord> documents = adminWorkspaceDao.getDocuments();
        typeFilterComboBox.getItems().setAll("Tous les types");
        documents.stream().map(AdminWorkspaceDao.AdminDocumentRecord::type).distinct().sorted().forEach(typeFilterComboBox.getItems()::add);
        typeFilterComboBox.setValue("Tous les types");

        patientFilterComboBox.getItems().setAll("Tous les patients");
        documents.stream().map(AdminWorkspaceDao.AdminDocumentRecord::patientName).distinct().sorted().forEach(patientFilterComboBox.getItems()::add);
        patientFilterComboBox.setValue("Tous les patients");
    }

    private void refreshDocuments() {
        String query = normalize(searchField.getText());
        String type = typeFilterComboBox.getValue();
        String patient = patientFilterComboBox.getValue();

        List<AdminWorkspaceDao.AdminDocumentRecord> documents = adminWorkspaceDao.getDocuments().stream()
                .filter(document -> query.isBlank()
                        || normalize(document.title()).contains(query)
                        || normalize(document.patientName()).contains(query)
                        || normalize(document.doctorName()).contains(query))
                .filter(document -> "Tous les types".equals(type) || document.type().equalsIgnoreCase(type))
                .filter(document -> "Tous les patients".equals(patient) || document.patientName().equalsIgnoreCase(patient))
                .toList();

        documentsCountLabel.setText(documents.size() + (documents.size() > 1 ? " document(s)" : " document"));
        documentsContainer.getChildren().clear();
        for (AdminWorkspaceDao.AdminDocumentRecord document : documents) {
            documentsContainer.getChildren().add(buildDocumentCard(document));
        }
    }

    private VBox buildDocumentCard(AdminWorkspaceDao.AdminDocumentRecord document) {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("admin-list-item");

        HBox row = new HBox(16);
        VBox details = new VBox(6);
        HBox titleRow = new HBox(10);
        Label title = new Label(document.title());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        Label type = new Label(document.type());
        type.getStyleClass().addAll("admin-badge", "admin-badge-info");
        titleRow.getChildren().addAll(title, type);
        Label meta = new Label("Patient: " + document.patientName() + "  •  Médecin: " + document.doctorName() + "  •  " + document.date());
        meta.getStyleClass().add("admin-muted");
        details.getChildren().addAll(titleRow, meta);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button viewButton = buildButton("Voir", "admin-secondary-button");
        viewButton.setOnAction(event -> showDocument(document));
        Button downloadButton = buildButton("Télécharger", "admin-secondary-button");
        downloadButton.setOnAction(event -> showDownloadInfo(document));
        Button deleteButton = buildButton("Supprimer", "admin-danger-button");
        deleteButton.setOnAction(event -> {
            adminWorkspaceDao.deleteDocument(document.id());
            reloadFilters();
            refreshDocuments();
        });
        actions.getChildren().addAll(viewButton, downloadButton, deleteButton);

        row.getChildren().addAll(details, actions);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private void showDocument(AdminWorkspaceDao.AdminDocumentRecord document) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Document");
        alert.setHeaderText(document.title());
        alert.setContentText(
                "Type: " + document.type() + "\n" +
                "Patient: " + document.patientName() + "\n" +
                "Médecin: " + document.doctorName() + "\n" +
                "Date: " + document.date() + "\n\n" +
                document.description()
        );
        alert.showAndWait();
    }

    private void showDownloadInfo(AdminWorkspaceDao.AdminDocumentRecord document) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Téléchargement");
        alert.setHeaderText(document.title());
        alert.setContentText(document.filePath().isBlank()
                ? "Aucun fichier physique n'est renseigné pour ce document."
                : "Chemin du fichier: " + document.filePath());
        alert.showAndWait();
    }

    private Button buildButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.FRENCH).trim();
    }
}
