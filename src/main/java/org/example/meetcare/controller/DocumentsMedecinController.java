package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.MedicalRecordDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.MedicalRecord;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.PrescriptionDocumentUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class DocumentsMedecinController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MedicalRecordDao medicalRecordDao = new MedicalRecordDao();
    private final UsersDao usersDao = new UsersDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final PrescriptionDocumentUtil prescriptionDocumentUtil = new PrescriptionDocumentUtil();

    private final List<MedicalRecord> allRecords = new ArrayList<>();
    @FXML private Button createPrescriptionButton;
    @FXML private Label totalDocumentsLabel;
    @FXML private Label prescriptionsCountLabel;
    @FXML private Label biologyImagingCountLabel;
    @FXML private Label patientsCountLabel;
    @FXML private Label filteredCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> patientFilterComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private Button allFilterButton;
    @FXML private Button ordonnanceFilterButton;
    @FXML private Button biologieFilterButton;
    @FXML private Button imagerieFilterButton;
    @FXML private VBox documentsListContainer;
    @FXML private WebView previewWebView;
    @FXML private VBox previewPlaceholder;
    @FXML private Label previewTitleLabel;
    @FXML private Label previewMetaLabel;
    @FXML private Label previewTypeLabel;
    @FXML private Label previewConsentLabel;

    private Users currentUser;
    private Integer currentDoctorId;
    private MedicalRecord selectedRecord;
    private String activeQuickFilter = "TOUS";

    @FXML
    private void initialize() {
        currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        currentDoctorId = doctorDao.findDoctorIdByUserId(currentUser.getIdUsers()).orElse(null);
        setupFilters();
        refreshRecords();
    }

    @FXML
    private void handleCreateDocument() {
        handleCreatePrescription();
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    @FXML
    private void handleFilterChange(ActionEvent event) {
        applyFilters();
    }

    @FXML
    private void handleSearchChange() {
        applyFilters();
    }

    @FXML
    private void handleQuickFilter(ActionEvent event) {
        Object source = event.getSource();
        if (source == ordonnanceFilterButton) {
            activeQuickFilter = "ORDONNANCE";
        } else if (source == biologieFilterButton) {
            activeQuickFilter = "BIOLOGY";
        } else if (source == imagerieFilterButton) {
            activeQuickFilter = "IMAGING";
        } else {
            activeQuickFilter = "TOUS";
        }
        updateQuickFilterStyles();
        applyFilters();
    }

    private void setupFilters() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        if (typeFilterComboBox != null) {
            typeFilterComboBox.setItems(FXCollections.observableArrayList(
                    "Tous les types",
                    "Ordonnance",
                    "Biologie",
                    "Imagerie",
                    "Analyse",
                    "Radio",
                    "IRM",
                    "Scanner",
                    "Echographie",
                    "Compte rendu",
                    "Certificat",
                    "Autre"
            ));
            typeFilterComboBox.setValue("Tous les types");
            typeFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        if (patientFilterComboBox != null) {
            patientFilterComboBox.setItems(FXCollections.observableArrayList("Tous les patients"));
            patientFilterComboBox.setValue("Tous les patients");
            patientFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        updateQuickFilterStyles();
    }

    private void refreshRecords() {
        if (currentUser == null) {
            return;
        }

        try {
            allRecords.clear();
            allRecords.addAll(medicalRecordDao.findSharedByDoctorUserId(currentUser.getIdUsers()));
            refreshStats();
            refreshPatientFilters();
            applyFilters();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement", rootCauseMessage(ex));
        }
    }

    private void refreshStats() {
        totalDocumentsLabel.setText(String.valueOf(allRecords.size()));
        prescriptionsCountLabel.setText(String.valueOf(countByType("ORDONNANCE")));
        biologyImagingCountLabel.setText(String.valueOf(countBiologyAndImaging()));
        patientsCountLabel.setText(String.valueOf(countDistinctPatients()));
    }

    private void refreshPatientFilters() {
        if (patientFilterComboBox == null) {
            return;
        }

        String selected = patientFilterComboBox.getValue();
        List<String> items = new ArrayList<>();
        items.add("Tous les patients");
        items.addAll(allRecords.stream()
                .map(record -> emptyIfBlank(record.getPatientName(), "Patient"))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());
        patientFilterComboBox.setItems(FXCollections.observableArrayList(items));
        if (selected != null && items.contains(selected)) {
            patientFilterComboBox.setValue(selected);
        } else {
            patientFilterComboBox.setValue("Tous les patients");
        }
    }

    private void applyFilters() {
        if (documentsListContainer == null) {
            return;
        }

        String query = normalizeText(searchField == null ? "" : searchField.getText());
        String patientFilter = patientFilterComboBox == null ? "Tous les patients" : patientFilterComboBox.getValue();
        String typeFilter = typeFilterComboBox == null ? "Tous les types" : typeFilterComboBox.getValue();

        List<MedicalRecord> filtered = allRecords.stream()
                .filter(record -> matchesSearch(record, query))
                .filter(record -> "Tous les patients".equalsIgnoreCase(patientFilter)
                        || emptyIfBlank(record.getPatientName(), "Patient").equalsIgnoreCase(patientFilter))
                .filter(record -> matchesType(record, typeFilter))
                .filter(this::matchesQuickFilter)
                .collect(Collectors.toList());

        filteredCountLabel.setText(filtered.size() + " document" + (filtered.size() > 1 ? "s" : ""));
        renderDocumentList(filtered);

        if (selectedRecord != null) {
            MedicalRecord stillVisible = filtered.stream()
                    .filter(record -> record.getId() == selectedRecord.getId())
                    .findFirst()
                    .orElse(null);
            if (stillVisible != null) {
                selectedRecord = stillVisible;
                renderPreview(stillVisible);
            } else if (!filtered.isEmpty()) {
                selectRecord(filtered.get(0));
            } else {
                clearPreview();
            }
        } else if (!filtered.isEmpty()) {
            selectRecord(filtered.get(0));
        } else {
            clearPreview();
        }
    }

    private void renderDocumentList(List<MedicalRecord> records) {
        if (documentsListContainer == null) {
            return;
        }

        documentsListContainer.getChildren().clear();
        if (records.isEmpty()) {
            documentsListContainer.getChildren().add(createEmptyState("Aucun document ne correspond a vos filtres."));
            return;
        }

        for (MedicalRecord record : records) {
            documentsListContainer.getChildren().add(createDocumentCard(record));
        }
    }

    private VBox createDocumentCard(MedicalRecord record) {
        boolean selected = selectedRecord != null && selectedRecord.getId() == record.getId();
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("document-card");
        card.setStyle(selected
                ? "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 18; -fx-padding: 16;"
                : "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 18; -fx-padding: 16;");
        card.setOnMouseClicked(event -> selectRecord(record));

        HBox topRow = new HBox(14);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.setMinSize(52, 52);
        icon.setMaxSize(52, 52);
        icon.setStyle("-fx-background-radius: 16; " + iconStyle(record.getDocumentType()));
        Label iconLabel = new Label(documentIcon(record.getDocumentType()));
        iconLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; " + iconTextStyle(record.getDocumentType()));
        icon.getChildren().add(iconLabel);

        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(emptyIfBlank(record.getTitle(), "Document medical"));
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label patient = new Label("Patient: " + emptyIfBlank(record.getPatientName(), "Patient"));
        patient.setStyle("-fx-font-size: 13px; -fx-text-fill: #2563eb; -fx-font-weight: 600;");

        Label meta = new Label(formatDateTime(record.getDate()) + "  |  " + emptyIfBlank(normalizeDisplayType(record.getDocumentType()), "Autre"));
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        info.getChildren().addAll(title, patient, meta);

        VBox badges = new VBox(6);
        badges.setAlignment(Pos.CENTER_RIGHT);

        Label typeBadge = new Label(normalizeDisplayType(record.getDocumentType()));
        typeBadge.setStyle(typeBadgeStyle(record.getDocumentType()));
        Label shareBadge = new Label(record.isShareConsent() && record.getSharedDoctorId() != null
                ? "Partage autorise"
                : "Acces medecin");
        shareBadge.setStyle(record.isShareConsent() && record.getSharedDoctorId() != null
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 999; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 999; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        badges.getChildren().addAll(typeBadge, shareBadge);

        topRow.getChildren().addAll(icon, info, badges);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button previewButton = new Button("Apercu");
        previewButton.setOnAction(event -> {
            event.consume();
            openPreviewDialog(record);
        });
        previewButton.setStyle(actionPrimaryStyle());

        Button editButton = new Button("✏ Modifier");
        editButton.setOnAction(event -> {
            event.consume();
            handleEditPrescription(record);
        });
        editButton.setStyle(actionSecondaryStyle());
        editButton.setVisible(isPrescription(record));
        editButton.setManaged(isPrescription(record));

        Button deleteButton = new Button("🗑 Supprimer");
        deleteButton.setOnAction(event -> {
            event.consume();
            handleDeletePrescription(record);
        });
        deleteButton.setStyle("-fx-background-color: #fff1f2; -fx-text-fill: #be123c; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #fecdd3; -fx-border-radius: 10; -fx-padding: 8 14;");
        deleteButton.setVisible(isPrescription(record));
        deleteButton.setManaged(isPrescription(record));

        Button downloadButton = new Button("Telecharger");
        downloadButton.setOnAction(event -> {
            event.consume();
            downloadDocument(record, getWindow());
        });
        downloadButton.setStyle(actionSecondaryStyle());

        actions.getChildren().addAll(spacer, previewButton, editButton, deleteButton, downloadButton);

        card.setUserData(record);
        card.getChildren().addAll(topRow, actions);
        return card;
    }

    private void selectRecord(MedicalRecord record) {
        selectedRecord = record;
        renderPreview(record);
        applySelectionStyles();
    }

    private void applySelectionStyles() {
        if (documentsListContainer == null) {
            return;
        }

        for (Node node : documentsListContainer.getChildren()) {
            if (node instanceof VBox card) {
                MedicalRecord record = (MedicalRecord) card.getUserData();
                boolean selected = selectedRecord != null && record != null && record.getId() == selectedRecord.getId();
                card.setStyle(selected
                        ? "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 18; -fx-padding: 16;"
                        : "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 18; -fx-padding: 16;");
            }
        }
    }

    private void renderPreview(MedicalRecord record) {
        if (previewPlaceholder != null) {
            previewPlaceholder.setManaged(false);
            previewPlaceholder.setVisible(false);
        }
        if (previewWebView != null) {
            previewWebView.setVisible(true);
            previewWebView.setManaged(true);
        }

        previewTitleLabel.setText(emptyIfBlank(record.getTitle(), "Document medical"));
        previewMetaLabel.setText("Patient: " + emptyIfBlank(record.getPatientName(), "Patient")
                + "  |  " + formatDateTime(record.getDate()));
        previewTypeLabel.setText(normalizeDisplayType(record.getDocumentType()));
        previewConsentLabel.setText(record.isShareConsent() && record.getSharedDoctorId() != null
                ? "Partage autorise au medecin designé"
                : "Document non partage");

        try {
            String html = buildPreviewHtml(record);
            previewWebView.getEngine().loadContent(html);
        } catch (Exception ex) {
            previewWebView.getEngine().loadContent(buildFallbackPreviewHtml(record, rootCauseMessage(ex)));
        }
    }

    private void openPreviewDialog(MedicalRecord record) {
        if (record == null) {
            return;
        }

        String html;
        try {
            html = buildPreviewHtml(record);
        } catch (Exception ex) {
            html = buildFallbackPreviewHtml(record, rootCauseMessage(ex));
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Apercu du document");
        dialog.setHeaderText(emptyIfBlank(record.getTitle(), "Document medical"));
        Window owner = getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        WebView webView = new WebView();
        webView.setPrefSize(1080, 760);
        webView.setMinSize(0, 0);
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.setContextMenuEnabled(false);
        webView.getEngine().loadContent(html);

        VBox content = new VBox(webView);
        content.setStyle("-fx-background-color: #f8fafc; -fx-padding: 0;");
        VBox.setVgrow(webView, Priority.ALWAYS);

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefSize(1120, 820);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setContent(content);
        pane.setStyle("-fx-background-color: #f8fafc;");

        dialog.showAndWait();
    }

    private void clearPreview() {
        selectedRecord = null;
        if (previewWebView != null) {
            previewWebView.getEngine().loadContent("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8" />
                        <style>
                            body {
                                margin: 0;
                                min-height: 100vh;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                background: #f8fafc;
                                color: #64748b;
                                font-family: Arial, Helvetica, sans-serif;
                            }
                            .card {
                                text-align: center;
                                padding: 36px;
                                background: white;
                                border: 1px dashed #cbd5e1;
                                border-radius: 18px;
                                max-width: 360px;
                            }
                            h3 {
                                margin: 0 0 8px 0;
                                color: #0f172a;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <h3>Apercu du document</h3>
                            <div>Selectionnez un document pour afficher son contenu ici.</div>
                        </div>
                    </body>
                    </html>
                    """);
        }

        if (previewTitleLabel != null) {
            previewTitleLabel.setText("Aucun document selectionne");
        }
        if (previewMetaLabel != null) {
            previewMetaLabel.setText("");
        }
        if (previewTypeLabel != null) {
            previewTypeLabel.setText("-");
        }
        if (previewConsentLabel != null) {
            previewConsentLabel.setText("");
        }
        if (previewPlaceholder != null) {
            previewPlaceholder.setManaged(true);
            previewPlaceholder.setVisible(true);
        }
    }

    private void downloadDocument(MedicalRecord record, Window owner) {
        Path source = Path.of(record.getFilePath());
        if (!Files.exists(source)) {
            showAlert(Alert.AlertType.ERROR, "Fichier introuvable", "Le fichier source n'existe pas.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le document");
        chooser.setInitialFileName(buildDownloadFileName(record, source));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }

        try {
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showAlert(Alert.AlertType.INFORMATION, "Document telecharge", "Le document a ete enregistre dans:\n" + target.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur de telechargement", rootCauseMessage(ex));
        }
    }

    private void handleEditPrescription(MedicalRecord record) {
        if (!isPrescription(record)) {
            return;
        }

        Window owner = getWindow();
        List<Users> patients = usersDao.findAllPatients();
        Optional<PrescriptionFormData> result = showPrescriptionDialog(owner, "Modifier l'ordonnance", record, patients);
        if (result.isEmpty()) {
            return;
        }

        try {
            savePrescription(record, result.get());
            refreshRecords();
            showAlert(Alert.AlertType.INFORMATION, "Ordonnance modifiee", "L'ordonnance a ete mise a jour.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
        }
    }

    private void handleDeletePrescription(MedicalRecord record) {
        if (!isPrescription(record)) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'ordonnance");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous supprimer definitivement cette ordonnance ?");
        Window owner = getWindow();
        if (owner != null) {
            confirm.initOwner(owner);
        }

        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        try {
            Path filePath = Path.of(record.getFilePath());
            medicalRecordDao.deleteById(record.getId());
            Files.deleteIfExists(filePath);
            if (selectedRecord != null && selectedRecord.getId() == record.getId()) {
                clearPreview();
            }
            refreshRecords();
            showAlert(Alert.AlertType.INFORMATION, "Ordonnance supprimee", "L'ordonnance a ete supprimee.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
        }
    }

    @FXML
    private void handleCreatePrescription(ActionEvent event) {
        handleCreatePrescription();
    }

    private void handleCreatePrescription() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun medecin connecte.");
            return;
        }

        List<Users> patients = usersDao.findAllPatients();
        if (patients.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Aucun patient", "Aucun patient disponible en base.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle ordonnance");
        dialog.setHeaderText(null);
        Window owner = getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType generateButtonType = new ButtonType("Generer l'ordonnance", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().setStyle("-fx-background-color: #f8fafc;");

        ComboBox<Users> patientBox = new ComboBox<>(FXCollections.observableArrayList(patients));
        patientBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Users user) {
                return user == null ? "" : buildDisplayName(user);
            }

            @Override
            public Users fromString(String string) {
                return null;
            }
        });
        patientBox.getSelectionModel().selectFirst();
        patientBox.setMaxWidth(Double.MAX_VALUE);

        TextField titleField = new TextField("Ordonnance");
        titleField.setMaxWidth(Double.MAX_VALUE);
        TextArea diagnosisArea = new TextArea();
        diagnosisArea.setPromptText("Ex: douleur thoracique, suivi post-operatoire, etc.");
        diagnosisArea.setPrefRowCount(3);
        diagnosisArea.setWrapText(true);
        diagnosisArea.setMaxWidth(Double.MAX_VALUE);

        TextArea treatmentArea = new TextArea();
        treatmentArea.setPromptText("Traitement, posologie, examens a effectuer...");
        treatmentArea.setPrefRowCount(4);
        treatmentArea.setWrapText(true);
        treatmentArea.setMaxWidth(Double.MAX_VALUE);

        TextArea instructionsArea = new TextArea();
        instructionsArea.setPromptText("Conseils, repos, signes d'alerte, suivi...");
        instructionsArea.setPrefRowCount(4);
        instructionsArea.setWrapText(true);
        instructionsArea.setMaxWidth(Double.MAX_VALUE);

        TextField validityField = new TextField("14 jours");
        validityField.setMaxWidth(Double.MAX_VALUE);

        Label headerTitle = new Label("Creation d'ordonnance");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSubtitle = new Label("Renseignez les informations qui seront injectees dans le template PDF.");
        headerSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.92);");

        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);
        headerBox.setStyle("-fx-background-color: linear-gradient(to right, #0f766e, #2563eb); -fx-background-radius: 16; -fx-padding: 18 20;");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(14);
        gridPane.setVgap(14);
        gridPane.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20;");

        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(150);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setFillWidth(true);
        gridPane.getColumnConstraints().addAll(left, right);

        gridPane.add(dialogLabel("Patient"), 0, 0);
        gridPane.add(patientBox, 1, 0);
        gridPane.add(dialogLabel("Titre"), 0, 1);
        gridPane.add(titleField, 1, 1);
        gridPane.add(dialogLabel("Diagnostic / motif"), 0, 2);
        gridPane.add(diagnosisArea, 1, 2);
        gridPane.add(dialogLabel("Traitement"), 0, 3);
        gridPane.add(treatmentArea, 1, 3);
        gridPane.add(dialogLabel("Instructions"), 0, 4);
        gridPane.add(instructionsArea, 1, 4);
        gridPane.add(dialogLabel("Validite"), 0, 5);
        gridPane.add(validityField, 1, 5);

        Label helper = new Label("L'ordonnance sera enregistrée comme document partage au patient selectionne.");
        helper.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        gridPane.add(helper, 1, 6);

        VBox content = new VBox(16, headerBox, gridPane);
        content.setStyle("-fx-padding: 6;");
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != generateButtonType) {
            return;
        }

        Users patient = patientBox.getValue();
        String title = valueOrEmpty(titleField.getText());
        String diagnosis = valueOrEmpty(diagnosisArea.getText());
        String treatment = valueOrEmpty(treatmentArea.getText());
        String instructions = valueOrEmpty(instructionsArea.getText());
        String validity = valueOrEmpty(validityField.getText());

        if (patient == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner un patient.");
            return;
        }
        if (diagnosis.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le motif ou diagnostic est obligatoire.");
            return;
        }
        if (treatment.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le traitement est obligatoire.");
            return;
        }

        Integer doctorId = currentDoctorId;
        if (doctorId == null) {
            doctorId = doctorDao.ensureDoctorIdForUser(currentUser.getIdUsers());
            currentDoctorId = doctorId;
        }

        String doctorName = buildDisplayName(currentUser);
        String specialty = doctorDao.findByUserId(currentUser.getIdUsers())
                .map(doctor -> doctor.getSpeciality())
                .orElse("Medecine generale");
        String patientName = buildDisplayName(patient);

        Path outputDir = Path.of(System.getProperty("user.dir"), "uploads", "medical-records", "prescriptions", "patient-" + patient.getIdUsers());
        try {
            Files.createDirectories(outputDir);
            Path outputPath = outputDir.resolve(prescriptionDocumentUtil.buildDefaultFileName(patientName, LocalDateTime.now()));
            prescriptionDocumentUtil.renderPrescription(new PrescriptionDocumentUtil.PrescriptionRequest(
                    doctorName,
                    specialty,
                    patientName,
                    diagnosis,
                    treatment,
                    instructions,
                    validity,
                    LocalDateTime.now()
            ), outputPath);

            MedicalRecord record = new MedicalRecord();
            record.setTitle(title.isBlank() ? "Ordonnance" : title);
            record.setDescription(diagnosis + System.lineSeparator() + treatment);
            record.setDocumentType("ORDONNANCE");
            record.setFilePath(outputPath.toAbsolutePath().normalize().toString());
            record.setIdDoctors(doctorId);
            record.setIdUsers(patient.getIdUsers());
            record.setShareConsent(true);
            record.setSharedDoctorId(doctorId);
            record.setPrescriptionDiagnosis(diagnosis);
            record.setPrescriptionTreatment(treatment);
            record.setPrescriptionInstructions(instructions);
            record.setPrescriptionValidity(validity);

            try {
                medicalRecordDao.insert(record);
            } catch (Exception insertEx) {
                Files.deleteIfExists(outputPath);
                throw insertEx;
            }

            refreshRecords();
            allRecords.stream()
                    .filter(item -> outputPath.toAbsolutePath().normalize().toString().equals(item.getFilePath()))
                    .findFirst()
                    .ifPresent(this::selectRecord);

            showAlert(Alert.AlertType.INFORMATION, "Ordonnance generee", "L'ordonnance a ete ajoutee aux documents du patient.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
        }
    }

    private Optional<PrescriptionFormData> showPrescriptionDialog(Window owner, String title, MedicalRecord existingRecord, List<Users> patients) {
        Dialog<PrescriptionFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType saveButtonType = new ButtonType(existingRecord == null ? "Generer l'ordonnance" : "Enregistrer les modifications", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(780);
        dialog.getDialogPane().setStyle("-fx-background-color: #f8fafc;");

        ComboBox<Users> patientBox = new ComboBox<>(FXCollections.observableArrayList(patients));
        patientBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Users user) {
                return user == null ? "" : buildDisplayName(user);
            }

            @Override
            public Users fromString(String string) {
                return null;
            }
        });
        patientBox.setMaxWidth(Double.MAX_VALUE);

        TextField titleField = new TextField();
        titleField.setPromptText("Titre de l'ordonnance");
        titleField.setMaxWidth(Double.MAX_VALUE);

        TextArea diagnosisArea = new TextArea();
        diagnosisArea.setPromptText("Motif ou diagnostic");
        diagnosisArea.setPrefRowCount(3);
        diagnosisArea.setWrapText(true);
        diagnosisArea.setMaxWidth(Double.MAX_VALUE);

        TextArea treatmentArea = new TextArea();
        treatmentArea.setPromptText("Une ligne par traitement. Format suggere: Medicament | Posologie | Duree | Remarques");
        treatmentArea.setPrefRowCount(5);
        treatmentArea.setWrapText(true);
        treatmentArea.setMaxWidth(Double.MAX_VALUE);

        TextArea instructionsArea = new TextArea();
        instructionsArea.setPromptText("Recommandations, suivi, alertes...");
        instructionsArea.setPrefRowCount(4);
        instructionsArea.setWrapText(true);
        instructionsArea.setMaxWidth(Double.MAX_VALUE);

        TextField validityField = new TextField();
        validityField.setPromptText("Ex: 3 mois");
        validityField.setMaxWidth(Double.MAX_VALUE);

        if (existingRecord != null) {
            titleField.setText(emptyIfBlank(existingRecord.getTitle(), ""));
            diagnosisArea.setText(emptyIfBlank(existingRecord.getPrescriptionDiagnosis(), existingRecord.getDescription()));
            treatmentArea.setText(emptyIfBlank(existingRecord.getPrescriptionTreatment(), existingRecord.getDescription()));
            instructionsArea.setText(emptyIfBlank(existingRecord.getPrescriptionInstructions(), ""));
            validityField.setText(emptyIfBlank(existingRecord.getPrescriptionValidity(), "3 mois"));
            patientBox.getItems().stream()
                    .filter(user -> user.getIdUsers() == existingRecord.getIdUsers())
                    .findFirst()
                    .ifPresent(patientBox.getSelectionModel()::select);
        } else {
            titleField.setText("Ordonnance");
            validityField.setText("3 mois");
            patientBox.getSelectionModel().selectFirst();
        }

        Label headerTitle = new Label(existingRecord == null ? "Creation d'ordonnance" : "Modification d'ordonnance");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSubtitle = new Label("Renseignez les informations qui seront injectees dans le PDF.");
        headerSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.92);");

        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);
        headerBox.setStyle("-fx-background-color: linear-gradient(to right, #0f766e, #2563eb); -fx-background-radius: 16; -fx-padding: 18 20;");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(14);
        gridPane.setVgap(14);
        gridPane.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20;");

        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(150);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setFillWidth(true);
        gridPane.getColumnConstraints().addAll(left, right);

        gridPane.add(dialogLabel("Patient"), 0, 0);
        gridPane.add(patientBox, 1, 0);
        gridPane.add(dialogLabel("Titre"), 0, 1);
        gridPane.add(titleField, 1, 1);
        gridPane.add(dialogLabel("Diagnostic"), 0, 2);
        gridPane.add(diagnosisArea, 1, 2);
        gridPane.add(dialogLabel("Traitement"), 0, 3);
        gridPane.add(treatmentArea, 1, 3);
        gridPane.add(dialogLabel("Instructions"), 0, 4);
        gridPane.add(instructionsArea, 1, 4);
        gridPane.add(dialogLabel("Validite"), 0, 5);
        gridPane.add(validityField, 1, 5);

        Label helper = new Label("Pour le traitement, une ligne correspond a un element du tableau PDF.");
        helper.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        gridPane.add(helper, 1, 6);

        VBox content = new VBox(16, headerBox, gridPane);
        content.setStyle("-fx-padding: 6;");
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            Users selectedPatient = patientBox.getValue();
            if (selectedPatient == null) {
                return null;
            }
            return new PrescriptionFormData(
                    selectedPatient,
                    valueOrEmpty(titleField.getText()),
                    valueOrEmpty(diagnosisArea.getText()),
                    valueOrEmpty(treatmentArea.getText()),
                    valueOrEmpty(instructionsArea.getText()),
                    valueOrEmpty(validityField.getText())
            );
        });

        Optional<PrescriptionFormData> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return result;
        }

        PrescriptionFormData data = result.get();
        if (data.patient() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner un patient.");
            return Optional.empty();
        }
        if (data.diagnosis().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le diagnostic est obligatoire.");
            return Optional.empty();
        }
        if (data.treatment().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le traitement est obligatoire.");
            return Optional.empty();
        }

        return result;
    }

    private MedicalRecord savePrescription(MedicalRecord existingRecord, PrescriptionFormData data) throws Exception {
        if (currentUser == null) {
            throw new IllegalStateException("Aucun medecin connecte.");
        }

        Integer doctorId = currentDoctorId;
        if (doctorId == null) {
            doctorId = doctorDao.ensureDoctorIdForUser(currentUser.getIdUsers());
            currentDoctorId = doctorId;
        }

        String doctorName = buildDisplayName(currentUser);
        String specialty = doctorDao.findByUserId(currentUser.getIdUsers())
                .map(doctor -> doctor.getSpeciality())
                .orElse("Medecine generale");
        String patientName = buildDisplayName(data.patient());

        Path outputDir = Path.of(System.getProperty("user.dir"), "uploads", "medical-records", "prescriptions", "patient-" + data.patient().getIdUsers());
        Files.createDirectories(outputDir);

        Path outputPath = outputDir.resolve(prescriptionDocumentUtil.buildDefaultFileName(patientName, LocalDateTime.now()));
        PrescriptionDocumentUtil.PrescriptionRequest request = buildPrescriptionRequest(doctorName, specialty, patientName, data);
        prescriptionDocumentUtil.renderPrescription(request, outputPath);

        MedicalRecord record = existingRecord == null ? new MedicalRecord() : existingRecord;
        Path previousPath = existingRecord == null || existingRecord.getFilePath() == null ? null : Path.of(existingRecord.getFilePath());

        record.setTitle(data.title().isBlank() ? "Ordonnance" : data.title());
        record.setDescription(data.diagnosis() + System.lineSeparator() + data.treatment());
        record.setDocumentType("ORDONNANCE");
        record.setFilePath(outputPath.toAbsolutePath().normalize().toString());
        record.setIdDoctors(doctorId);
        record.setIdUsers(data.patient().getIdUsers());
        record.setShareConsent(true);
        record.setSharedDoctorId(doctorId);
        record.setPrescriptionDiagnosis(data.diagnosis());
        record.setPrescriptionTreatment(data.treatment());
        record.setPrescriptionInstructions(data.instructions());
        record.setPrescriptionValidity(data.validity());

        try {
            if (existingRecord == null) {
                medicalRecordDao.insert(record);
            } else {
                medicalRecordDao.update(record);
            }
        } catch (Exception ex) {
            Files.deleteIfExists(outputPath);
            throw ex;
        }

        if (previousPath != null && !previousPath.toAbsolutePath().normalize().equals(outputPath.toAbsolutePath().normalize())) {
            Files.deleteIfExists(previousPath);
        }

        return record;
    }

    private PrescriptionDocumentUtil.PrescriptionRequest buildPrescriptionRequest(String doctorName, String specialty, String patientName, PrescriptionFormData data) {
        return new PrescriptionDocumentUtil.PrescriptionRequest(
                doctorName,
                specialty,
                patientName,
                data.diagnosis(),
                data.treatment(),
                data.instructions(),
                data.validity(),
                LocalDateTime.now()
        );
    }

    private PrescriptionDocumentUtil.PrescriptionRequest buildPrescriptionRequest(MedicalRecord record) {
        String doctorName = buildDisplayName(currentUser);
        String specialty = doctorDao.findByUserId(currentUser.getIdUsers())
                .map(doctor -> doctor.getSpeciality())
                .orElse("Medecine generale");
        return new PrescriptionDocumentUtil.PrescriptionRequest(
                doctorName,
                specialty,
                emptyIfBlank(record.getPatientName(), "Patient"),
                emptyIfBlank(record.getPrescriptionDiagnosis(), record.getDescription()),
                emptyIfBlank(record.getPrescriptionTreatment(), record.getDescription()),
                emptyIfBlank(record.getPrescriptionInstructions(), ""),
                emptyIfBlank(record.getPrescriptionValidity(), "3 mois"),
                record.getDate()
        );
    }

    private boolean isPrescription(MedicalRecord record) {
        return record != null && "ORDONNANCE".equals(normalizeType(record.getDocumentType()));
    }

    private void selectRecordByPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        allRecords.stream()
                .filter(item -> filePath.equals(item.getFilePath()))
                .findFirst()
                .ifPresent(this::selectRecord);
    }

    private record PrescriptionFormData(Users patient, String title, String diagnosis, String treatment, String instructions, String validity) {
    }

    private boolean matchesSearch(MedicalRecord record, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return normalizeText(record.getTitle()).contains(query)
                || normalizeText(record.getDescription()).contains(query)
                || normalizeText(record.getPatientName()).contains(query)
                || normalizeText(record.getDocumentType()).contains(query);
    }

    private boolean matchesType(MedicalRecord record, String typeFilter) {
        if (typeFilter == null || typeFilter.isBlank() || "Tous les types".equalsIgnoreCase(typeFilter)) {
            return true;
        }
        return normalizeDisplayType(record.getDocumentType()).equalsIgnoreCase(typeFilter)
                || ("Biologie".equalsIgnoreCase(typeFilter) && isBiologyOrImaging(record.getDocumentType()))
                || ("Imagerie".equalsIgnoreCase(typeFilter) && isImaging(record.getDocumentType()));
    }

    private boolean matchesQuickFilter(MedicalRecord record) {
        return switch (activeQuickFilter) {
            case "ORDONNANCE" -> "ORDONNANCE".equals(normalizeType(record.getDocumentType()));
            case "BIOLOGY" -> isBiology(record.getDocumentType());
            case "IMAGING" -> isImaging(record.getDocumentType());
            default -> true;
        };
    }

    private int countByType(String type) {
        return (int) allRecords.stream()
                .filter(record -> type.equals(normalizeType(record.getDocumentType())))
                .count();
    }

    private int countBiologyAndImaging() {
        return (int) allRecords.stream()
                .filter(record -> isBiologyOrImaging(record.getDocumentType()))
                .count();
    }

    private int countDistinctPatients() {
        return (int) allRecords.stream()
                .map(record -> emptyIfBlank(record.getPatientName(), String.valueOf(record.getIdUsers())))
                .distinct()
                .count();
    }

    private boolean isBiologyOrImaging(String type) {
        return isBiology(type) || isImaging(type);
    }

    private boolean isBiology(String type) {
        String normalized = normalizeType(type);
        return "ANALYSE".equals(normalized);
    }

    private boolean isImaging(String type) {
        String normalized = normalizeType(type);
        return switch (normalized) {
            case "RADIO", "IRM", "SCANNER", "ECHOGRAPHIE" -> true;
            default -> false;
        };
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            return "OTHER";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDisplayType(String value) {
        if (value == null || value.isBlank()) {
            return "Autre";
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ORDONNANCE" -> "Ordonnance";
            case "RADIO" -> "Imagerie";
            case "ANALYSE" -> "Biologie";
            case "SCANNER" -> "Imagerie";
            case "IRM" -> "Imagerie";
            case "ECHOGRAPHIE" -> "Imagerie";
            case "COMPTE_RENDU" -> "Compte rendu";
            case "CERTIFICAT" -> "Certificat";
            default -> "Autre";
        };
    }

    private String buildPreviewHtml(MedicalRecord record) throws Exception {
        Path path = Path.of(record.getFilePath());
        if (!Files.exists(path)) {
            throw new IOException("Le fichier source n'existe pas.");
        }

        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        String displayName = escapeHtml(emptyIfBlank(record.getTitle(), path.getFileName().toString()));
        String patientName = escapeHtml(emptyIfBlank(record.getPatientName(), "Patient"));
        String date = escapeHtml(formatDateTime(record.getDate()));
        String documentType = escapeHtml(normalizeDisplayType(record.getDocumentType()));

        String content;
        if (lower.endsWith(".pdf")) {
            content = renderPdfToHtml(path);
        } else if (isImage(lower)) {
            content = """
                    <div style="padding:18px;background:#f8fafc;">
                        <img src="%s" alt="%s" style="width:100%%;height:auto;display:block;border-radius:12px;background:white;border:1px solid #e5e7eb;" />
                    </div>
                    """.formatted(path.toUri(), displayName);
        } else if (isText(lower)) {
            content = """
                    <pre style="white-space:pre-wrap;word-break:break-word;background:white;border:1px solid #e5e7eb;border-radius:12px;padding:18px;line-height:1.6;">%s</pre>
                    """.formatted(escapeHtml(Files.readString(path, StandardCharsets.UTF_8)));
        } else {
            content = """
                    <div style="padding:18px;background:white;border:1px solid #e5e7eb;border-radius:12px;">
                        <h3 style="margin-top:0;">Apercu non disponible</h3>
                        <p>Ce format ne peut pas etre affiche directement. Utilisez le bouton Telecharger.</p>
                    </div>
                    """;
        }

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <style>
                        body {
                            margin: 0;
                            background: #f8fafc;
                            font-family: Arial, Helvetica, sans-serif;
                            color: #0f172a;
                            overflow-x: hidden;
                        }
                        .wrap {
                            padding: 12px;
                            overflow-x: hidden;
                        }
                        .card {
                            background: white;
                            border-radius: 18px;
                            border: 1px solid #e5e7eb;
                            box-shadow: 0 18px 35px rgba(15, 23, 42, 0.08);
                            overflow: hidden;
                            width: 100%%;
                            box-sizing: border-box;
                        }
                        .header {
                            padding: 18px 20px;
                            border-bottom: 1px solid #eef2f7;
                        }
                        h1 {
                            margin: 0 0 6px 0;
                            font-size: 22px;
                        }
                        .meta {
                            font-size: 13px;
                            color: #64748b;
                            line-height: 1.6;
                        }
                        .badge {
                            display: inline-block;
                            margin-top: 6px;
                            padding: 5px 10px;
                            border-radius: 999px;
                            background: #eff6ff;
                            color: #1d4ed8;
                            font-size: 11px;
                            font-weight: bold;
                        }
                        .viewer {
                            min-height: 540px;
                            background: #f8fafc;
                            padding: 16px;
                            overflow-x: hidden;
                        }
                        .summary {
                            padding-top: 12px;
                            border-top: 1px solid #eef2f7;
                            margin-top: 12px;
                            font-size: 13px;
                            color: #475569;
                        }
                    </style>
                </head>
                <body>
                    <div class="wrap">
                        <div class="card">
                            <div class="header">
                                <h1>%s</h1>
                                <div class="meta">Patient: %s<br />Date: %s</div>
                                <div class="badge">%s</div>
                                <div class="summary">Document partage au medecin depuis l'interface patient.</div>
                            </div>
                            <div class="viewer">%s</div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(displayName, patientName, date, documentType, content);
    }

    private String buildFallbackPreviewHtml(MedicalRecord record, String error) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        body {
                            margin: 0;
                            padding: 24px;
                            background: #f8fafc;
                            font-family: Arial, Helvetica, sans-serif;
                        }
                        .card {
                            background: white;
                            border: 1px solid #e5e7eb;
                            border-radius: 16px;
                            padding: 20px;
                        }
                        h3 {
                            margin: 0 0 10px 0;
                        }
                        p {
                            margin: 0;
                            color: #64748b;
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h3>Impossible d'afficher l'apercu</h3>
                        <p>%s</p>
                    </div>
                </body>
                </html>
                """.formatted(escapeHtml(error == null ? "Erreur inconnue" : error));
    }

    private String renderPdfToHtml(Path path) throws Exception {
        StringBuilder html = new StringBuilder();
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 220, ImageType.RGB);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
                html.append("""
                        <div style="margin:0 0 16px 0;background:white;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;box-shadow:0 10px 20px rgba(15,23,42,0.06);max-width:100%%;box-sizing:border-box;">
                            <img alt="page %d" src="data:image/png;base64,%s" style="width:100%%;max-width:100%%;height:auto;display:block;" />
                        </div>
                        """.formatted(pageIndex + 1, base64));
            }
        }
        return html.toString();
    }

    private boolean isImage(String lower) {
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif")
                || lower.endsWith(".bmp");
    }

    private boolean isText(String lower) {
        return lower.endsWith(".txt") || lower.endsWith(".csv") || lower.endsWith(".log");
    }

    private String documentIcon(String type) {
        return switch (normalizeType(type)) {
            case "ORDONNANCE" -> "Rx";
            case "ANALYSE" -> "⚗";
            case "RADIO", "IRM", "SCANNER", "ECHOGRAPHIE" -> "▣";
            case "CERTIFICAT" -> "✓";
            default -> "PDF";
        };
    }

    private String iconStyle(String type) {
        return switch (normalizeType(type)) {
            case "ORDONNANCE" -> "-fx-background-color: #eff6ff;";
            case "ANALYSE" -> "-fx-background-color: #ecfdf5;";
            case "RADIO", "IRM", "SCANNER", "ECHOGRAPHIE" -> "-fx-background-color: #fff7ed;";
            case "CERTIFICAT" -> "-fx-background-color: #fefce8;";
            default -> "-fx-background-color: #f8fafc;";
        };
    }

    private String iconTextStyle(String type) {
        return switch (normalizeType(type)) {
            case "ORDONNANCE" -> "color: #2563eb;";
            case "ANALYSE" -> "color: #15803d;";
            case "RADIO", "IRM", "SCANNER", "ECHOGRAPHIE" -> "color: #ea580c;";
            case "CERTIFICAT" -> "color: #ca8a04;";
            default -> "color: #475569;";
        };
    }

    private String typeBadgeStyle(String type) {
        return switch (normalizeType(type)) {
            case "ORDONNANCE" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "ANALYSE" -> "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "RADIO", "IRM", "SCANNER", "ECHOGRAPHIE" -> "-fx-background-color: #ffedd5; -fx-text-fill: #ea580c; -fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "CERTIFICAT" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
            default -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 999; -fx-padding: 5 10; -fx-font-size: 11px; -fx-font-weight: bold;";
        };
    }

    private String actionPrimaryStyle() {
        return "-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #bfdbfe; -fx-border-radius: 10; -fx-padding: 8 14;";
    }

    private String actionSecondaryStyle() {
        return "-fx-background-color: white; -fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-padding: 8 14;";
    }

    private String buildDownloadFileName(MedicalRecord record, Path source) {
        String base = emptyIfBlank(record.getTitle(), source.getFileName().toString());
        String safe = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        String extension = extractExtension(source.getFileName().toString());
        return safe + extension;
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index);
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (prenom.isEmpty() && username.isEmpty()) {
            return "Utilisateur";
        }
        if (prenom.isEmpty()) {
            return username;
        }
        if (username.isEmpty()) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String emptyIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Label dialogLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        return label;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 16; -fx-background-radius: 16;");
        Label title = new Label("Aucun document");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label subtitle = new Label(message);
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        subtitle.setWrapText(true);
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private void updateQuickFilterStyles() {
        applyQuickFilterStyle(allFilterButton, "TOUS");
        applyQuickFilterStyle(ordonnanceFilterButton, "ORDONNANCE");
        applyQuickFilterStyle(biologieFilterButton, "BIOLOGY");
        applyQuickFilterStyle(imagerieFilterButton, "IMAGING");
    }

    private void applyQuickFilterStyle(Button button, String filter) {
        if (button == null) {
            return;
        }
        boolean active = filter.equals(activeQuickFilter);
        button.setStyle(active
                ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-background-radius: 999; -fx-border-color: #bfdbfe; -fx-border-radius: 999; -fx-padding: 8 14; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-background-color: white; -fx-text-fill: #334155; -fx-background-radius: 999; -fx-border-color: #d1d5db; -fx-border-radius: 999; -fx-padding: 8 14; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    private Window getWindow() {
        if (createPrescriptionButton != null && createPrescriptionButton.getScene() != null) {
            return createPrescriptionButton.getScene().getWindow();
        }
        if (previewWebView != null && previewWebView.getScene() != null) {
            return previewWebView.getScene().getWindow();
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Window owner = getWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
