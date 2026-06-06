package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Invoice;
import org.example.meetcare.model.Users;
import org.example.meetcare.service.InvoiceService;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;
import org.example.meetcare.utils.InvoiceStatus;
import org.example.meetcare.utils.LogoutUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class InvoicePatientController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> PAYMENT_METHODS = List.of("Carte bancaire", "Especes", "Virement bancaire");

    private final InvoiceService invoiceService = new InvoiceService();
    private final UsersDao usersDao = new UsersDao();

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarInitialsLabel;

    @FXML
    private Label totalPendingAmountLabel;

    @FXML
    private Label pendingInvoicesCountLabel;

    @FXML
    private Label paidInvoicesCountLabel;

    @FXML
    private VBox pendingInvoicesContainer;

    @FXML
    private VBox paidInvoicesContainer;

    private Users currentUser;

    @FXML
    private void initialize() {
        currentUser = refreshCurrentUser();
        AvatarUtil.applyAvatar(avatarCircle, avatarInitialsLabel, currentUser);
        refreshView();
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }

    private void refreshView() {
        List<Invoice> invoices;
        try {
            invoices = invoiceService.loadPatientInvoices(currentUser);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
            return;
        }

        List<Invoice> pendingInvoices = new ArrayList<>();
        List<Invoice> paidInvoices = new ArrayList<>();

        for (Invoice invoice : invoices) {
            if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
                paidInvoices.add(invoice);
            } else {
                pendingInvoices.add(invoice);
            }
        }

        BigDecimal totalPendingAmount = pendingInvoices.stream()
                .map(invoice -> invoice.getAmount() == null ? BigDecimal.ZERO : invoice.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalPendingAmountLabel.setText(formatMoney(totalPendingAmount));
        pendingInvoicesCountLabel.setText(String.valueOf(pendingInvoices.size()));
        paidInvoicesCountLabel.setText(String.valueOf(paidInvoices.size()));

        pendingInvoicesContainer.getChildren().clear();
        paidInvoicesContainer.getChildren().clear();

        if (pendingInvoices.isEmpty()) {
            pendingInvoicesContainer.getChildren().add(createEmptyState("Aucune facture en attente."));
        } else {
            for (Invoice invoice : pendingInvoices) {
                pendingInvoicesContainer.getChildren().add(createInvoiceCard(invoice, true));
            }
        }

        if (paidInvoices.isEmpty()) {
            paidInvoicesContainer.getChildren().add(createEmptyState("Aucune facture payee."));
        } else {
            for (Invoice invoice : paidInvoices) {
                paidInvoicesContainer.getChildren().add(createInvoiceCard(invoice, false));
            }
        }
    }

    private HBox createInvoiceCard(Invoice invoice, boolean pending) {
        HBox card = new HBox(14);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(pending
                ? "-fx-background-color: #FFF3EC; -fx-background-radius: 12; -fx-padding: 18 20 18 20; -fx-border-color: #F5D5C0; -fx-border-radius: 12; -fx-border-width: 1;"
                : "-fx-background-color: #F0FAF4; -fx-background-radius: 12; -fx-padding: 18 20 18 20; -fx-border-color: #B8DFC8; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane icon = new StackPane();
        Circle circle = new Circle(22);
        circle.setStyle(pending ? "-fx-fill: #F5D5C0;" : "-fx-fill: #C2E8D0;");
        Label iconLabel = new Label("$");
        iconLabel.setStyle(pending ? "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #E05A2B;"
                : "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3DAA70;");
        icon.getChildren().addAll(circle, iconLabel);

        VBox info = new VBox(3);
        Label doctorName = new Label(invoice.getDoctorName());
        doctorName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1A1A;");
        Label speciality = new Label(invoice.getDoctorSpeciality());
        speciality.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");
        Label details = new Label((invoice.getCreatedAt() == null ? "" : DATE_FORMATTER.format(invoice.getCreatedAt().toLocalDate()))
                + "   " + invoice.getDescription());
        details.setStyle("-fx-font-size: 11px; -fx-text-fill: #AAAAAA;");
        info.getChildren().addAll(doctorName, speciality, details);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox amountBox = new VBox(4);
        amountBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label amountLabel = new Label(formatMoney(invoice.getAmount()));
        amountLabel.setStyle(pending ? "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E05A2B;"
                : "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3DAA70;");
        Label statusLabel = new Label(pending ? " En attente " : " Payee ");
        statusLabel.setStyle(pending
                ? "-fx-font-size: 11px; -fx-text-fill: #E05A2B; -fx-background-color: #FAE0D0; -fx-background-radius: 5; -fx-padding: 2 8 2 8;"
                : "-fx-font-size: 11px; -fx-text-fill: #3DAA70; -fx-background-color: #C8EDD8; -fx-background-radius: 5; -fx-padding: 2 8 2 8;");
        amountBox.getChildren().addAll(amountLabel, statusLabel);

        VBox actionColumn = new VBox(8);
        actionColumn.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionButtons.getChildren().addAll(
                createPreviewButton(invoice),
                createDownloadButton(invoice)
        );
        actionColumn.getChildren().add(actionButtons);

        if (pending) {
            Button payButton = new Button("Payer");
            payButton.setStyle("-fx-background-color: #E05A2B; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 18 8 18;");
            payButton.setOnAction(event -> handlePayInvoice(invoice));
            actionColumn.getChildren().add(payButton);
        }

        card.getChildren().addAll(icon, info, amountBox, actionColumn);
        return card;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18 20 18 20; -fx-border-color: #EEEEEE; -fx-border-radius: 10; -fx-border-width: 1;");
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #666666;");
        box.getChildren().add(label);
        return box;
    }

    private void handlePayInvoice(Invoice invoice) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(PAYMENT_METHODS.get(0), PAYMENT_METHODS);
        dialog.setTitle("Paiement");
        dialog.setHeaderText("Paiement de la facture de " + invoice.getDoctorName());
        dialog.setContentText("Choisissez le mode de paiement");
        Window owner = getOwnerWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            invoiceService.payInvoice(invoice.getId(), result.get());
            refreshView();
            showAlert(Alert.AlertType.INFORMATION, "Paiement valide", "La facture a ete marquee comme payee.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", rootCauseMessage(ex));
        }
    }

    private Button createPreviewButton(Invoice invoice) {
        Button button = new Button("Apercu");
        button.setStyle("-fx-background-color: #EEF2FF; -fx-text-fill: #4F46E5; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #C7D2FE; "
                + "-fx-border-radius: 8; -fx-padding: 8 14 8 14;");
        button.setOnAction(event -> {
            try {
                new org.example.meetcare.utils.InvoiceDocumentUtil().showPreview(invoice, resolveWindow(event.getSource()));
            } catch (NoClassDefFoundError error) {
                showAlert(Alert.AlertType.WARNING, "Apercu indisponible",
                        "Le module JavaFX Web n'est pas charge dans ce lancement.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Apercu indisponible", rootCauseMessage(ex));
            }
        });
        return button;
    }

    private Button createDownloadButton(Invoice invoice) {
        Button button = new Button("Telecharger");
        button.setStyle("-fx-background-color: white; -fx-text-fill: #444444; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #CCCCCC; "
                + "-fx-border-radius: 8; -fx-padding: 8 14 8 14;");
        button.setOnAction(event -> {
            try {
                new org.example.meetcare.utils.InvoiceDocumentUtil().downloadPdf(invoice, resolveWindow(event.getSource()));
            } catch (NoClassDefFoundError error) {
                showAlert(Alert.AlertType.WARNING, "Telechargement indisponible",
                        "Le module JavaFX Web n'est pas charge dans ce lancement.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur PDF", rootCauseMessage(ex));
            }
        });
        return button;
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

    private Window getOwnerWindow() {
        if (avatarCircle != null && avatarCircle.getScene() != null) {
            return avatarCircle.getScene().getWindow();
        }
        return null;
    }

    private Window resolveWindow(Object source) {
        if (source instanceof Node node && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return getOwnerWindow();
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
