package org.example.meetcare.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Invoice;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.InvoiceService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.InvoiceDocumentUtil;
import org.example.meetcare.utils.InvoiceStatus;
import org.example.meetcare.utils.LogoutUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class InvoiceMedecinController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InvoiceService invoiceService = new InvoiceService();
    private final UsersDao usersDao = new UsersDao();
    private final InvoiceDocumentUtil invoiceDocumentUtil = new InvoiceDocumentUtil();

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private Button createInvoiceButton;

    @FXML
    private Label totalInvoicesLabel;

    @FXML
    private Label paidRevenueLabel;

    @FXML
    private Label pendingInvoicesLabel;

    @FXML
    private Label paidInvoicesLabel;

    @FXML
    private VBox invoiceListContainer;

    private Users currentUser;

    @FXML
    private void initialize() {
        currentUser = refreshCurrentUser();
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, currentUser);
        refreshView();
    }

    @FXML
    private void handleCreateInvoice() {
        List<Users> patients = invoiceService.loadEligiblePatients();
        if (patients.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Aucun patient", "Aucun patient disponible en base.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Creer une facture");
        dialog.setHeaderText(null);
        Window owner = getOwnerWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType createButtonType = new ButtonType("Generer la facture", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().setStyle("-fx-background-color: #F3F6FB;");

        ComboBox<Users> patientBox = new ComboBox<>(FXCollections.observableArrayList(patients));
        patientBox.setPromptText("Selectionner un patient");
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

        TextField amountField = new TextField();
        amountField.setPromptText("Ex: 25.00");
        amountField.setMaxWidth(Double.MAX_VALUE);

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Ex: Consultation generale, suivi, acte medical...");
        descriptionField.setPrefRowCount(4);
        descriptionField.setWrapText(true);
        descriptionField.setMaxWidth(Double.MAX_VALUE);

        Label headerTitle = new Label("Creation de facture");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSubtitle = new Label("Renseignez les informations qui alimenteront le template PDF.");
        headerSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.92);");

        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);
        headerBox.setStyle("-fx-background-color: linear-gradient(to right, #8B5CF6, #4F46E5);"
                + "-fx-background-radius: 16; -fx-padding: 18 20 18 20;");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(14);
        gridPane.setVgap(14);
        gridPane.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20;");

        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setMinWidth(140);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setHgrow(Priority.ALWAYS);
        rightColumn.setFillWidth(true);
        gridPane.getColumnConstraints().addAll(leftColumn, rightColumn);

        gridPane.add(createDialogLabel("Patient"), 0, 0);
        gridPane.add(patientBox, 1, 0);
        gridPane.add(createDialogLabel("Montant"), 0, 1);
        gridPane.add(amountField, 1, 1);
        gridPane.add(createDialogLabel("Description"), 0, 2);
        gridPane.add(descriptionField, 1, 2);

        Label helperLabel = new Label("Le document sera rendu en HTML/CSS puis exporte en PDF.");
        helperLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
        gridPane.add(helperLabel, 1, 3);

        VBox formWrapper = new VBox(16, headerBox, gridPane);
        formWrapper.setStyle("-fx-padding: 6;");
        dialog.getDialogPane().setContent(formWrapper);

        try {
            Optional<ButtonType> dialogResult = dialog.showAndWait();
            if (dialogResult.isEmpty() || dialogResult.get() != createButtonType) {
                return;
            }

            Users selectedPatient = patientBox.getValue();
            BigDecimal amount = parseAmount(amountField.getText());
            String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();

            if (selectedPatient == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez selectionner un patient.");
                return;
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez saisir un montant valide.");
                return;
            }
            if (description.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "La description est obligatoire.");
                return;
            }

            invoiceService.createInvoice(currentUser, selectedPatient, amount, description);
            refreshView();
            showAlert(Alert.AlertType.INFORMATION, "Facture creee", "La facture a ete ajoutee aux factures en attente.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    private void refreshView() {
        List<Invoice> invoices;
        try {
            invoices = invoiceService.loadDoctorInvoices(currentUser);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
            return;
        }

        totalInvoicesLabel.setText(String.valueOf(invoices.size()));
        long pendingCount = invoices.stream().filter(invoice -> InvoiceStatus.PENDING.equals(invoice.getStatus())).count();
        long paidCount = invoices.stream().filter(invoice -> InvoiceStatus.PAID.equals(invoice.getStatus())).count();
        BigDecimal paidRevenue = invoices.stream()
                .filter(invoice -> InvoiceStatus.PAID.equals(invoice.getStatus()))
                .map(invoice -> invoice.getAmount() == null ? BigDecimal.ZERO : invoice.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pendingInvoicesLabel.setText(String.valueOf(pendingCount));
        paidInvoicesLabel.setText(String.valueOf(paidCount));
        paidRevenueLabel.setText(formatMoney(paidRevenue));

        invoiceListContainer.getChildren().clear();
        if (invoices.isEmpty()) {
            invoiceListContainer.getChildren().add(createEmptyState("Aucune facture pour le moment."));
            return;
        }

        for (Invoice invoice : invoices) {
            invoiceListContainer.getChildren().add(createInvoiceCard(invoice));
        }
    }

    private HBox createInvoiceCard(Invoice invoice) {
        boolean pending = InvoiceStatus.PENDING.equals(invoice.getStatus());
        HBox card = new HBox(16);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18 20 18 20; "
                + "-fx-border-color: #EEEEEE; -fx-border-radius: 10; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, #e8e8e8, 4, 0, 0, 1);");

        StackPane icon = new StackPane();
        Circle circle = new Circle(18);
        circle.setStyle(pending ? "-fx-fill: #FDEBD0;" : "-fx-fill: #D4EDDA;");
        Label iconLabel = new Label("$");
        iconLabel.setStyle(pending ? "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #E05A2B;"
                : "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3DAA70;");
        icon.getChildren().addAll(circle, iconLabel);

        VBox info = new VBox(3);
        Label patientLabel = new Label(invoice.getPatientName());
        patientLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A1A1A;");
        Label descriptionLabel = new Label(invoice.getDescription());
        descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999999;");
        Label dateLabel = new Label(invoice.getCreatedAt() == null ? "" : DATE_FORMATTER.format(invoice.getCreatedAt().toLocalDate()));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #AAAAAA;");
        info.getChildren().addAll(patientLabel, descriptionLabel, dateLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox amountBox = new VBox(5);
        amountBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        amountBox.setMinWidth(120);
        Label amountLabel = new Label(formatMoney(invoice.getAmount()));
        amountLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1A1A1A;");
        Label statusLabel = new Label(pending ? " En attente " : " Payee ");
        statusLabel.setStyle(pending
                ? "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #E05A2B; -fx-background-color: #FDEBD0; -fx-background-radius: 5; -fx-padding: 3 8 3 8;"
                : "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3DAA70; -fx-background-color: #D4EDDA; -fx-background-radius: 5; -fx-padding: 3 8 3 8;");
        amountBox.getChildren().addAll(amountLabel, statusLabel);

        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actions.getChildren().addAll(
                invoiceDocumentUtil.createPreviewButton(invoice),
                invoiceDocumentUtil.createDownloadButton(invoice)
        );

        card.getChildren().addAll(icon, info, amountBox, actions);
        return card;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18 20 18 20; "
                + "-fx-border-color: #EEEEEE; -fx-border-radius: 10; -fx-border-width: 1;");
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #666666;");
        box.getChildren().add(label);
        return box;
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.trim().replace(',', '.').replace("€", "").trim();
        try {
            return new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildDisplayName(Users user) {
        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (prenom.isEmpty() && username.isEmpty()) {
            return "Patient";
        }
        if (prenom.isEmpty()) {
            return username;
        }
        if (username.isEmpty()) {
            return prenom;
        }
        return prenom + " " + username;
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        DecimalFormat format = new DecimalFormat("0.00", symbols);
        return format.format(value) + " \u20AC";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Window owner = getOwnerWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private Label createDialogLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        return label;
    }

    private Window getOwnerWindow() {
        if (createInvoiceButton != null && createInvoiceButton.getScene() != null) {
            return createInvoiceButton.getScene().getWindow();
        }
        if (avatarCircle != null && avatarCircle.getScene() != null) {
            return avatarCircle.getScene().getWindow();
        }
        return null;
    }

    private Users refreshCurrentUser() {
        Users user = AppSession.getCurrentUser();
        if (user == null || user.getIdUsers() <= 0) {
            return user;
        }

        Users freshUser = usersDao.findById(user.getIdUsers()).orElse(user);
        AppSession.setCurrentUser(freshUser);
        return freshUser;
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
