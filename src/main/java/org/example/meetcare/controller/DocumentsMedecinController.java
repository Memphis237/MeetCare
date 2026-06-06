package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.meetcare.dao.DoctorWorkspaceDao;
import org.example.meetcare.model.MedicalDocumentData;
import org.example.meetcare.service.MedicalDocumentFormService;
import org.example.meetcare.utils.DocumentPreviewUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DocumentsMedecinController {
    private static final String ALL_PATIENTS = "Tous les patients";
    private static final String ALL_TYPES = "Tous les types";
    private static final DateTimeFormatter EXPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ComboBox<String> patientFilterComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private Label totalDocumentsLabel;
    @FXML private Label patientsCountLabel;
    @FXML private Label documentTypesCountLabel;
    @FXML private Label documentsCountLabel;
    @FXML private VBox recentDocumentsContainer;
    @FXML private VBox olderDocumentsContainer;
    @FXML private Button actionAnchorButton;

    private final DoctorWorkspaceDao doctorWorkspaceDao = new DoctorWorkspaceDao();

    @FXML
    private void initialize() {
        populateFilters();
        patientFilterComboBox.setOnAction(event -> refreshDocuments());
        typeFilterComboBox.setOnAction(event -> refreshDocuments());
        refreshDocuments();
    }

    @FXML
    private void handleCreateDocument() {
        List<DoctorWorkspaceDao.PatientRecord> patients = doctorWorkspaceDao.getPatients();
        if (patients.isEmpty()) {
            showInfo("Aucun patient", "Ajoutez d'abord des rendez-vous avant de creer des documents.");
            return;
        }

        DoctorWorkspaceDao.PatientRecord patient = patients.stream()
                .filter(item -> item.name().equals(patientFilterComboBox.getValue()))
                .findFirst()
                .orElse(patients.get(0));

        Optional<MedicalDocumentData> result = MedicalDocumentFormService.showDocumentDialog(patient.name());
        result.ifPresent(document -> {
            doctorWorkspaceDao.insertMedicalDocument(patient.id(), document);
            populateFilters();
            refreshDocuments();
        });
    }

    private void populateFilters() {
        List<DoctorWorkspaceDao.DocumentRecord> documents = doctorWorkspaceDao.getDocuments();

        List<String> patients = new ArrayList<>();
        patients.add(ALL_PATIENTS);
        documents.stream()
                .map(DoctorWorkspaceDao.DocumentRecord::patientName)
                .distinct()
                .sorted()
                .forEach(patients::add);

        List<String> types = new ArrayList<>();
        types.add(ALL_TYPES);
        documents.stream()
                .map(DoctorWorkspaceDao.DocumentRecord::type)
                .distinct()
                .sorted()
                .forEach(types::add);

        patientFilterComboBox.setItems(FXCollections.observableArrayList(patients));
        typeFilterComboBox.setItems(FXCollections.observableArrayList(types));

        if (patientFilterComboBox.getValue() == null || !patients.contains(patientFilterComboBox.getValue())) {
            patientFilterComboBox.setValue(ALL_PATIENTS);
        }
        if (typeFilterComboBox.getValue() == null || !types.contains(typeFilterComboBox.getValue())) {
            typeFilterComboBox.setValue(ALL_TYPES);
        }
    }

    private void refreshDocuments() {
        String patient = patientFilterComboBox.getValue();
        String type = typeFilterComboBox.getValue();

        List<DoctorWorkspaceDao.DocumentRecord> filtered = doctorWorkspaceDao.getDocuments().stream()
                .filter(document -> ALL_PATIENTS.equals(patient) || document.patientName().equals(patient))
                .filter(document -> ALL_TYPES.equals(type) || document.type().equals(type))
                .toList();

        updateSummaryCards(filtered);
        documentsCountLabel.setText(filtered.size() + (filtered.size() > 1 ? " document(s)" : " document"));

        recentDocumentsContainer.getChildren().clear();
        olderDocumentsContainer.getChildren().clear();

        if (filtered.isEmpty()) {
            recentDocumentsContainer.getChildren().add(createEmptyState(
                    "Aucun document pour le moment",
                    "Les documents apparaitront ici quand vous enverrez un document a un patient."
            ));
            return;
        }

        List<DoctorWorkspaceDao.DocumentRecord> recentDocuments = filtered.stream().limit(3).toList();
        List<DoctorWorkspaceDao.DocumentRecord> olderDocuments = filtered.stream().skip(3).toList();

        for (DoctorWorkspaceDao.DocumentRecord document : recentDocuments) {
            recentDocumentsContainer.getChildren().add(createDocumentCard(document, true));
        }

        if (olderDocuments.isEmpty()) {
            olderDocumentsContainer.getChildren().add(createEmptyState(
                    "Aucun autre document",
                    "Les documents les plus recents sont affiches ci-dessus."
            ));
        } else {
            for (DoctorWorkspaceDao.DocumentRecord document : olderDocuments) {
                olderDocumentsContainer.getChildren().add(createDocumentCard(document, false));
            }
        }
    }

    private void updateSummaryCards(List<DoctorWorkspaceDao.DocumentRecord> filtered) {
        totalDocumentsLabel.setText(String.valueOf(filtered.size()));
        patientsCountLabel.setText(String.valueOf(filtered.stream()
                .map(DoctorWorkspaceDao.DocumentRecord::patientName)
                .distinct()
                .count()));
        documentTypesCountLabel.setText(String.valueOf(filtered.stream()
                .map(DoctorWorkspaceDao.DocumentRecord::type)
                .distinct()
                .count()));
    }

    private HBox createDocumentCard(DoctorWorkspaceDao.DocumentRecord document, boolean highlighted) {
        HBox card = new HBox(14);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(highlighted
                ? "-fx-background-color: #F5F3FF; -fx-background-radius: 12; -fx-padding: 18 20 18 20; -fx-border-color: #DDD6FE; -fx-border-radius: 12; -fx-border-width: 1;"
                : "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 18 20 18 20; -fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane icon = new StackPane();
        Circle circle = new Circle(22);
        circle.setStyle(resolveCircleStyle(document.type()));
        Label iconLabel = new Label(resolveDocumentGlyph(document.type()));
        iconLabel.setStyle(resolveGlyphStyle(document.type()));
        icon.getChildren().addAll(circle, iconLabel);

        VBox info = new VBox(3);
        Label title = new Label(document.title());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1A1A;");
        Label patientName = new Label(document.patientName());
        patientName.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Label description = new Label(document.date() + "   " + document.description());
        description.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        info.getChildren().addAll(title, patientName, description);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox metaBox = new VBox(4);
        metaBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label status = new Label(document.status());
        status.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #16A34A;");
        Label typeBadge = new Label(document.type());
        typeBadge.getStyleClass().add(resolveBadgeStyle(document.type()));
        metaBox.getChildren().addAll(status, typeBadge);

        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionButtons.getChildren().addAll(
                createPreviewButton(document),
                createDownloadButton(document)
        );

        card.getChildren().addAll(icon, info, metaBox, actionButtons);
        return card;
    }

    private VBox createEmptyState(String titleText, String subtitleText) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18 20 18 20; -fx-border-color: #EEEEEE; -fx-border-radius: 10; -fx-border-width: 1;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        subtitle.setWrapText(true);

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Button createPreviewButton(DoctorWorkspaceDao.DocumentRecord document) {
        Button button = new Button("Apercu");
        button.setStyle("-fx-background-color: #EEF2FF; -fx-text-fill: #4F46E5; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #C7D2FE; "
                + "-fx-border-radius: 8; -fx-padding: 8 14 8 14;");
        button.setOnAction(event -> handlePreview(document, resolveWindow(event.getSource())));
        return button;
    }

    private Button createDownloadButton(DoctorWorkspaceDao.DocumentRecord document) {
        Button button = new Button("Telecharger");
        button.setStyle("-fx-background-color: white; -fx-text-fill: #444444; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #CCCCCC; "
                + "-fx-border-radius: 8; -fx-padding: 8 14 8 14;");
        button.setOnAction(event -> handleDownload(document, resolveWindow(event.getSource())));
        return button;
    }

    private void handlePreview(DoctorWorkspaceDao.DocumentRecord document, Window owner) {
        try {
            Path documentPath = resolveDocumentPath(document.filePath());
            if (documentPath != null && Files.exists(documentPath)) {
                DocumentPreviewUtil.showPreview(owner, documentPath.toString(), document.title());
                return;
            }
            showGeneratedPreview(document, owner);
        } catch (NoClassDefFoundError error) {
            showInfo("Apercu indisponible", "Le module JavaFX Web n'est pas charge dans ce lancement.");
        } catch (Exception ex) {
            showInfo("Apercu indisponible", rootCauseMessage(ex));
        }
    }

    private void handleDownload(DoctorWorkspaceDao.DocumentRecord document, Window owner) {
        try {
            Path source = resolveDocumentPath(document.filePath());
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer le document");

            if (source != null && Files.exists(source)) {
                chooser.setInitialFileName(source.getFileName().toString());
                File destination = chooser.showSaveDialog(owner);
                if (destination == null) {
                    return;
                }
                Files.copy(source, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showInfo("Telechargement termine", "Le document a ete enregistre.");
                return;
            }

            chooser.setInitialFileName(sanitizeFileName(document.title()) + ".html");
            File destination = chooser.showSaveDialog(owner);
            if (destination == null) {
                return;
            }
            Files.writeString(destination.toPath(), buildGeneratedPreviewHtml(document), StandardCharsets.UTF_8);
            showInfo("Export termine", "Le document a ete exporte en HTML.");
        } catch (Exception ex) {
            showInfo("Erreur de telechargement", rootCauseMessage(ex));
        }
    }

    private void showGeneratedPreview(DoctorWorkspaceDao.DocumentRecord document, Window owner) {
        try {
            Path tempFile = Files.createTempFile("meetcare-doctor-document-", ".html");
            Files.writeString(tempFile, buildGeneratedPreviewHtml(document), StandardCharsets.UTF_8);
            DocumentPreviewUtil.showPreview(owner, tempFile.toString(), document.title());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String buildGeneratedPreviewHtml(DoctorWorkspaceDao.DocumentRecord document) {
        String previewDate = document.date().isBlank() ? EXPORT_DATE_FORMATTER.format(LocalDateTime.now()) : document.date();
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            padding: 40px;
                            background: #F3F4F6;
                            font-family: Arial, sans-serif;
                            color: #0F172A;
                        }
                        .sheet {
                            max-width: 1100px;
                            margin: 0 auto;
                            background: white;
                            border: 1px solid #E5E7EB;
                            box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
                            padding: 42px;
                        }
                        .top {
                            display: flex;
                            justify-content: space-between;
                            align-items: flex-start;
                            margin-bottom: 40px;
                        }
                        .brand {
                            font-size: 54px;
                            font-weight: bold;
                            color: #111827;
                        }
                        .brand span {
                            color: #2563EB;
                        }
                        .meta {
                            text-align: right;
                            color: #64748B;
                            font-size: 18px;
                            line-height: 1.6;
                        }
                        .title {
                            margin-top: 28px;
                            font-size: 38px;
                            font-weight: bold;
                            color: #0F172A;
                        }
                        .subtitle {
                            font-size: 18px;
                            color: #64748B;
                            margin-bottom: 30px;
                        }
                        .grid {
                            display: grid;
                            grid-template-columns: 1fr 1fr;
                            gap: 28px;
                            margin-bottom: 34px;
                        }
                        .card {
                            background: #F8FAFC;
                            border: 1px solid #E2E8F0;
                            border-radius: 18px;
                            padding: 24px;
                        }
                        .label {
                            font-size: 14px;
                            letter-spacing: 0.08em;
                            color: #2563EB;
                            font-weight: bold;
                            text-transform: uppercase;
                            margin-bottom: 10px;
                        }
                        .value {
                            font-size: 18px;
                            line-height: 1.7;
                            color: #0F172A;
                        }
                        .description {
                            margin-top: 20px;
                            background: #F5F3FF;
                            border: 1px solid #DDD6FE;
                            border-radius: 18px;
                            padding: 26px;
                        }
                        .description h3 {
                            margin: 0 0 12px 0;
                            color: #0F172A;
                            font-size: 24px;
                        }
                        .description p {
                            margin: 0;
                            font-size: 18px;
                            line-height: 1.7;
                            color: #334155;
                            white-space: pre-wrap;
                        }
                    </style>
                </head>
                <body>
                    <div class="sheet">
                        <div class="top">
                            <div>
                                <div class="brand">Meet<span>Care</span></div>
                                <div class="subtitle">Document medical medecin</div>
                            </div>
                            <div class="meta">
                                <div><strong>Date</strong> %s</div>
                                <div><strong>Type</strong> %s</div>
                            </div>
                        </div>
                        <div class="title">%s</div>
                        <div class="grid">
                            <div class="card">
                                <div class="label">Patient</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="card">
                                <div class="label">Statut</div>
                                <div class="value">%s</div>
                            </div>
                        </div>
                        <div class="description">
                            <h3>Description</h3>
                            <p>%s</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(document.title()),
                escapeHtml(previewDate),
                escapeHtml(document.type()),
                escapeHtml(document.title()),
                escapeHtml(document.patientName()),
                escapeHtml(document.status()),
                escapeHtml(document.description().isBlank() ? "Aucune description fournie." : document.description())
        );
    }

    private Path resolveDocumentPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        Path path = Path.of(filePath);
        if (path.isAbsolute()) {
            return path;
        }
        return Path.of(System.getProperty("user.dir")).resolve(path).normalize();
    }

    private Window resolveWindow(Object source) {
        if (source instanceof Node node && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return null;
    }

    private String resolveBadgeStyle(String type) {
        return switch (type) {
            case "Ordonnance" -> "doctor-badge-blue";
            case "Compte rendu" -> "doctor-badge-purple";
            case "Analyse" -> "doctor-badge-green";
            case "Radio", "Scan" -> "doctor-badge-orange";
            default -> "doctor-badge-outline";
        };
    }

    private String resolveCircleStyle(String type) {
        return switch (type) {
            case "Ordonnance" -> "-fx-fill: #DBEAFE;";
            case "Compte rendu" -> "-fx-fill: #F3E8FF;";
            case "Analyse" -> "-fx-fill: #DCFCE7;";
            case "Radio", "Scan" -> "-fx-fill: #FFEDD5;";
            default -> "-fx-fill: #E2E8F0;";
        };
    }

    private String resolveGlyphStyle(String type) {
        return switch (type) {
            case "Ordonnance" -> "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2563EB;";
            case "Compte rendu" -> "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #9333EA;";
            case "Analyse" -> "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #16A34A;";
            case "Radio", "Scan" -> "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #EA580C;";
            default -> "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #475569;";
        };
    }

    private String resolveDocumentGlyph(String type) {
        return switch (type) {
            case "Ordonnance" -> "Rx";
            case "Compte rendu" -> "CR";
            case "Analyse" -> "AN";
            case "Radio" -> "RD";
            case "Scan" -> "SC";
            default -> "DOC";
        };
    }

    private String sanitizeFileName(String value) {
        return value == null ? "document_meetcare" : value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
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

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MeetCare");
        alert.setHeaderText(title);
        alert.setContentText(content);
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
}
