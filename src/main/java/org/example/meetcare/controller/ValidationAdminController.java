package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import org.example.meetcare.dao.DoctorDao;
import org.example.meetcare.dao.DoctorDocumentDao;
import org.example.meetcare.dao.NotificationDao;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.DoctorDocumentEntry;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.DocumentPreviewUtil;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValidationAdminController {
    private static final Set<String> REQUIRED_SINGLE_DOCUMENTS = Set.of(
            "CARTE_VITALE",
            "JUSTIFICATIF_DOMICILE",
            "ASSURANCE_HABITATION"
    );
    private static final Set<String> REQUIRED_ONE_OF_DOCUMENTS = Set.of(
            "TITRE_SEJOUR",
            "PASSEPORT",
            "VISA"
    );

    @FXML
    private Label pendingCountLabel;

    @FXML
    private VBox cardsContainer;

    @FXML
    private SidebarAdminController sidebarAdminController;

    private final DoctorDocumentDao doctorDocumentDao = new DoctorDocumentDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final UsersDao usersDao = new UsersDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final Map<Integer, CheckBox> selectionBoxesByDocumentId = new HashMap<>();

    @FXML
    private void initialize() {
        loadPendingDoctors();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadPendingDoctors();
    }

    private void loadPendingDoctors() {
        if (cardsContainer == null) {
            return;
        }

        List<DoctorDocumentEntry> entries = doctorDocumentDao.findPendingDocumentsForAdmin();
        Map<Integer, List<DoctorDocumentEntry>> groupedByDoctor = new LinkedHashMap<>();
        for (DoctorDocumentEntry entry : entries) {
            groupedByDoctor.computeIfAbsent(entry.getDoctorId(), key -> new ArrayList<>()).add(entry);
        }

        cardsContainer.getChildren().clear();
        selectionBoxesByDocumentId.clear();
        updatePendingCount(groupedByDoctor.size());
        refreshSidebar();

        if (groupedByDoctor.isEmpty()) {
            Label empty = new Label("Aucune demande de validation en attente.");
            empty.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
            cardsContainer.getChildren().add(empty);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Map.Entry<Integer, List<DoctorDocumentEntry>> doctorEntry : groupedByDoctor.entrySet()) {
            int doctorId = doctorEntry.getKey();
            List<DoctorDocumentEntry> documents = doctorEntry.getValue();
            DoctorDocumentEntry first = documents.get(0);

            VBox card = new VBox(14);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-padding: 20; -fx-border-color: #E5E7EB; -fx-border-radius: 18; -fx-effect: dropshadow(gaussian, rgba(17,24,39,0.08), 12, 0, 0, 2);");

            HBox header = new HBox(14);
            Rectangle avatarBackground = new Rectangle(54, 54);
            avatarBackground.setArcHeight(16);
            avatarBackground.setArcWidth(16);
            avatarBackground.setStyle("-fx-fill: linear-gradient(to bottom right, #DBEAFE, #BFDBFE);");

            Label initials = new Label(initialsOf(first.getUsername()));
            initials.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1D4ED8;");
            VBox avatarBox = new VBox();
            avatarBox.setMinSize(54, 54);
            avatarBox.setMaxSize(54, 54);
            avatarBox.setAlignment(javafx.geometry.Pos.CENTER);
            avatarBox.getChildren().addAll(avatarBackground, initials);

            VBox info = new VBox(4);
            Label name = new Label(displayName(first));
            name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label email = new Label(first.getEmail() == null ? "" : first.getEmail());
            email.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

            Label documentsCount = new Label(documents.size() + " document(s) envoye(s)");
            documentsCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #2563EB; -fx-font-weight: bold;");

            info.getChildren().addAll(name, email, documentsCount);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label status = new Label("EN ATTENTE");
            status.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 6 12 6 12;");

            header.getChildren().addAll(avatarBox, info, status);

            VBox docList = new VBox(8);
            for (DoctorDocumentEntry document : documents) {
                docList.getChildren().add(buildDocumentRow(document, formatter));
            }

            List<String> missingDocuments = missingRequiredDocuments(documents);
            Label requirementLabel = new Label(buildRequirementLabel(missingDocuments));
            requirementLabel.setWrapText(true);
            requirementLabel.setStyle(missingDocuments.isEmpty()
                        ? "-fx-background-color: #E8F8EF; -fx-text-fill: #15803D; -fx-background-radius: 12; -fx-padding: 10 12 10 12; -fx-font-size: 12px;"
                        : "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-background-radius: 12; -fx-padding: 10 12 10 12; -fx-font-size: 12px;");

            HBox actions = new HBox(12);
            Button approve = new Button("Valider la sélection");
            approve.setStyle("-fx-background-color: #16A34A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 18 10 18;");
            approve.setOnAction(event -> handleApprove(first.getDoctorId(), first.getUserId(), documents));

            Button reject = new Button("Refuser la sélection");
            reject.setStyle("-fx-background-color: white; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 18 10 18; -fx-border-color: #FCA5A5; -fx-border-radius: 10; -fx-border-width: 1.4;");
            reject.setOnAction(event -> handleReject(first.getDoctorId(), first.getUserId(), documents));

            actions.getChildren().addAll(approve, reject);

            card.getChildren().addAll(header, new Separator(), docList, requirementLabel, actions);
            cardsContainer.getChildren().add(card);
        }
    }

    private HBox buildDocumentRow(DoctorDocumentEntry document, DateTimeFormatter formatter) {
        HBox row = new HBox(10);
        row.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 12; -fx-padding: 12; -fx-border-color: #EEF2F7; -fx-border-radius: 12;");

        CheckBox selector = new CheckBox();
        selector.setStyle("-fx-cursor: hand;");
        selectionBoxesByDocumentId.put(document.getIdDoctorDocument(), selector);

        Node chip = createDocumentMarker(document.getDocumentType());

        VBox text = new VBox(2);
        Label title = new Label(document.getDocumentType() + " - " + document.getFileName());
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label meta = new Label("Ajoute le " + (document.getUploadedAt() == null ? "-" : formatter.format(document.getUploadedAt())));
        meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        Label status = new Label(document.getVerificationStatus());
        status.setStyle(documentStatusStyle(document.getVerificationStatus()));

        Button preview = new Button("Prévisualiser");
        preview.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
        preview.setOnAction(event -> DocumentPreviewUtil.showPreview(getWindow(event), document.getFilePath(), document.getFileName()));

        VBox right = new VBox(8, status, preview);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        text.getChildren().addAll(title, meta);
        HBox.setHgrow(text, Priority.ALWAYS);
        row.getChildren().addAll(selector, chip, text, right);
        return row;
    }

    private void handleApprove(int doctorId, int userId, List<DoctorDocumentEntry> documents) {
        Users admin = AppSession.getCurrentUser();
        if (admin == null) {
            showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun administrateur connecte.");
            return;
        }

        List<DoctorDocumentEntry> selectedDocuments = selectedDocumentsFor(documents);
        if (selectedDocuments.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner au moins un document a valider.");
            loadPendingDoctors();
            return;
        }

        List<Integer> selectedIds = selectedDocuments.stream().map(DoctorDocumentEntry::getIdDoctorDocument).toList();
        doctorDocumentDao.setVerificationStatusForDocuments(selectedIds, "APPROVED");
        doctorDao.saveVerificationDecision(doctorId, admin.getIdUsers(), "APPROVED",
                "Documents valides: " + documentSummary(selectedDocuments));

        if (isDossierFullyApproved(userId)) {
            usersDao.updateAccountStatus(userId, "VERIFIED");
            notificationDao.createNotification(userId,
                    "Compte valide",
                    "Bravo ! Votre dossier est complet et votre compte a ete valide.",
                    "SYSTEM");
            showAlert(Alert.AlertType.INFORMATION, "Validation effectuee", "Les documents selectionnes ont ete valides et le dossier est complet.");
        } else {
            usersDao.updateAccountStatus(userId, "PENDING");
            notificationDao.createNotification(userId,
                    "Documents valides",
                    "Les documents selectionnes ont ete valides. Vous pouvez continuer la finalisation de votre dossier.",
                    "SYSTEM");
            showAlert(Alert.AlertType.INFORMATION, "Validation effectuee", "Les documents selectionnes ont ete valides.");
        }
        loadPendingDoctors();
        refreshSidebar();
    }

    private void handleReject(int doctorId, int userId, List<DoctorDocumentEntry> documents) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refus du compte");
        dialog.setHeaderText("Indiquez la raison du refus pour les documents selectionnes");
        dialog.setContentText("Motif:");
        dialog.getEditor().setPromptText("Ex: diplome manquant, document illisible...");

        dialog.showAndWait().ifPresent(reason -> {
            String trimmed = reason == null ? "" : reason.trim();
            if (trimmed.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Motif requis", "Veuillez saisir une raison de refus.");
                return;
            }

            Users admin = AppSession.getCurrentUser();
            if (admin == null) {
                showAlert(Alert.AlertType.WARNING, "Session invalide", "Aucun administrateur connecte.");
                return;
            }

            List<DoctorDocumentEntry> selectedDocuments = selectedDocumentsFor(documents);
            if (selectedDocuments.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner au moins un document a refuser.");
                return;
            }

            List<Integer> selectedIds = selectedDocuments.stream().map(DoctorDocumentEntry::getIdDoctorDocument).toList();
            doctorDocumentDao.setVerificationStatusForDocuments(selectedIds, "REJECTED");
            doctorDao.saveVerificationDecision(doctorId, admin.getIdUsers(), "REJECTED",
                    trimmed + " | Documents refuses: " + documentSummary(selectedDocuments));
            usersDao.updateAccountStatus(userId, "PENDING");
            notificationDao.createNotification(userId,
                    "Documents refuses",
                    "Votre dossier contient des elements refuses: " + trimmed + ". Merci de renvoyer les documents corrigees.",
                    "SYSTEM");
            showAlert(Alert.AlertType.INFORMATION, "Documents refuses", "Les documents selectionnes ont ete refuses. Le medecin peut les renvoyer.");
            loadPendingDoctors();
            refreshSidebar();
        });
    }

    private void updatePendingCount(int count) {
        if (pendingCountLabel != null) {
            pendingCountLabel.setText(count + " en attente");
        }
    }

    private void refreshSidebar() {
        if (sidebarAdminController != null) {
            sidebarAdminController.refreshSidebar(AppSession.getCurrentUser());
        }
    }

    private String documentStatusStyle(String status) {
        if ("APPROVED".equalsIgnoreCase(status)) {
            return "-fx-background-color: #E8F8EF; -fx-text-fill: #15803D; -fx-background-radius: 999; -fx-font-weight: bold; -fx-padding: 5 10 5 10;";
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-background-radius: 999; -fx-font-weight: bold; -fx-padding: 5 10 5 10;";
        }
        return "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-background-radius: 999; -fx-font-weight: bold; -fx-padding: 5 10 5 10;";
    }

    private String displayName(DoctorDocumentEntry entry) {
        String username = entry.getUsername() == null ? "" : entry.getUsername().trim();
        String email = entry.getEmail() == null ? "" : entry.getEmail().trim();
        if (!username.isEmpty()) {
            return username;
        }
        return email.isEmpty() ? "Medecin" : email;
    }

    private String initialsOf(String value) {
        if (value == null || value.isBlank()) {
            return "D";
        }
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<DoctorDocumentEntry> selectedDocumentsFor(List<DoctorDocumentEntry> documents) {
        List<DoctorDocumentEntry> selected = new ArrayList<>();
        for (DoctorDocumentEntry document : documents) {
            CheckBox selector = selectionBoxesByDocumentId.get(document.getIdDoctorDocument());
            if (selector != null && selector.isSelected()) {
                selected.add(document);
            }
        }
        return selected;
    }

    private String documentSummary(List<DoctorDocumentEntry> documents) {
        return documents.stream()
                .map(document -> document.getDocumentType() + " - " + document.getFileName())
                .reduce((left, right) -> left + ", " + right)
                .orElse("aucun document");
    }

    private Node createDocumentMarker(String documentType) {
        if (!isMandatoryDocument(documentType)) {
            Region spacer = new Region();
            spacer.setMinWidth(12);
            spacer.setPrefWidth(12);
            spacer.setMaxWidth(12);
            return spacer;
        }

        Rectangle chip = new Rectangle(12, 12);
        chip.setArcHeight(4);
        chip.setArcWidth(4);
        chip.setStyle("-fx-fill: #60A5FA;");
        return chip;
    }

    private boolean isMandatoryDocument(String documentType) {
        String normalized = normalize(documentType);
        return REQUIRED_SINGLE_DOCUMENTS.contains(normalized) || REQUIRED_ONE_OF_DOCUMENTS.contains(normalized);
    }

    private boolean isDossierFullyApproved(int userId) {
        List<DoctorDocumentEntry> allDocuments = doctorDocumentDao.findByDoctorUserId(userId);
        Set<String> approvedTypes = new HashSet<>();
        for (DoctorDocumentEntry document : allDocuments) {
            if ("APPROVED".equalsIgnoreCase(document.getVerificationStatus())) {
                approvedTypes.add(normalize(document.getDocumentType()));
            }
        }

        for (String required : REQUIRED_SINGLE_DOCUMENTS) {
            if (!approvedTypes.contains(required)) {
                return false;
            }
        }

        return REQUIRED_ONE_OF_DOCUMENTS.stream().anyMatch(approvedTypes::contains);
    }

    private Window getWindow(ActionEvent event) {
        return ((Node) event.getSource()).getScene().getWindow();
    }

    private List<String> missingRequiredDocuments(List<DoctorDocumentEntry> documents) {
        Set<String> types = new HashSet<>();
        for (DoctorDocumentEntry document : documents) {
            if ("APPROVED".equalsIgnoreCase(document.getVerificationStatus())) {
                types.add(normalize(document.getDocumentType()));
            }
        }

        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_SINGLE_DOCUMENTS) {
            if (!types.contains(required)) {
                missing.add(required);
            }
        }

        boolean hasIdentityDocument = REQUIRED_ONE_OF_DOCUMENTS.stream().anyMatch(types::contains);
        if (!hasIdentityDocument) {
            missing.add("TITRE_SEJOUR / PASSEPORT / VISA");
        }

        return missing;
    }

    private String buildRequirementLabel(List<String> missingDocuments) {
        if (missingDocuments.isEmpty()) {
            return "Dossier complet: validation autorisee.";
        }
        return "Pieces obligatoires manquantes: " + String.join(", ", missingDocuments);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
