package org.example.meetcare.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.meetcare.model.Invoice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class InvoiceDocumentUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void showPreview(Invoice invoice, Window owner) {
        try {
            String html = buildHtml(invoice);

            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.getEngine().loadContent(html);
            webView.setPrefSize(960, 680);

            VBox content = new VBox(webView);
            content.setStyle("-fx-background-color: #F3F6FB; -fx-padding: 0;");
            VBox.setVgrow(webView, javafx.scene.layout.Priority.ALWAYS);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Apercu de la facture");
            dialog.setHeaderText("Visualisation HTML/CSS du document avant telechargement PDF");
            if (owner != null) {
                dialog.initOwner(owner);
            }

            DialogPane pane = dialog.getDialogPane();
            pane.setPrefSize(1020, 760);
            pane.getButtonTypes().add(ButtonType.CLOSE);
            pane.setContent(content);
            pane.setStyle("-fx-background-color: #F3F6FB;");

            dialog.showAndWait();
        } catch (Exception ex) {
            showError(owner, "Apercu indisponible", "Impossible d'ouvrir l'aperçu.\n" + rootCauseMessage(ex));
            ex.printStackTrace();
        }
    }

    public void downloadPdf(Invoice invoice, Window owner) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer la facture PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fileChooser.setInitialFileName(buildDefaultFileName(invoice));

            File target = fileChooser.showSaveDialog(owner);
            if (target == null) {
                return;
            }

            String html = buildHtml(invoice);
            renderPdf(html, target.toPath());
            showInfo(owner, "PDF genere", "La facture a ete enregistree dans:\n" + target.getAbsolutePath());
        } catch (Exception ex) {
            showError(owner, "Erreur PDF", rootCauseMessage(ex));
            ex.printStackTrace();
        }
    }

    public Button createPreviewButton(Invoice invoice) {
        Button button = new Button("Apercu");
        button.setStyle(primaryActionStyle());
        button.setOnAction(event -> {
            try {
                showPreview(invoice, resolveWindow(event.getSource()));
            } catch (Exception ex) {
                showError(resolveWindow(event.getSource()), "Apercu indisponible", rootCauseMessage(ex));
                ex.printStackTrace();
            }
        });
        return button;
    }

    public Button createDownloadButton(Invoice invoice) {
        Button button = new Button("Telecharger");
        button.setStyle(secondaryActionStyle());
        button.setOnAction(event -> {
            try {
                downloadPdf(invoice, resolveWindow(event.getSource()));
            } catch (Exception ex) {
                showError(resolveWindow(event.getSource()), "Erreur PDF", rootCauseMessage(ex));
                ex.printStackTrace();
            }
        });
        return button;
    }

    private void renderPdf(String html, Path outputPath) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, outputPath.getParent() == null
                    ? Path.of("").toAbsolutePath().toUri().toString()
                    : outputPath.getParent().toUri().toString());
            builder.toStream(outputStream);
            builder.run();
        }
    }

    private String buildHtml(Invoice invoice) {
        String invoiceNumber = buildInvoiceNumber(invoice);
        String issueDate = formatDateTime(invoice.getCreatedAt());
        String dueDate = invoice.getPaidAt() == null ? "En attente de reglement" : formatDateTime(invoice.getPaidAt());
        String doctorName = escapeHtml(valueOrDefault(invoice.getDoctorName(), "Medecin"));
        String doctorSpeciality = escapeHtml(valueOrDefault(invoice.getDoctorSpeciality(), "Specialite non renseignee"));
        String patientName = escapeHtml(valueOrDefault(invoice.getPatientName(), "Patient"));
        String description = escapeHtml(valueOrDefault(invoice.getDescription(), "Consultation"));
        String amount = formatMoney(invoice.getAmount());
        String statusLabel = "PAID".equalsIgnoreCase(invoice.getStatus()) ? "Payee" : "En attente";
        String paymentMethod = escapeHtml(valueOrDefault(invoice.getPaymentMethod(), "Non renseigne"));
        String paidAt = invoice.getPaidAt() == null ? "Non payee" : formatDateTime(invoice.getPaidAt());

        String html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        @page {
                            size: A4;
                            margin: 22mm 18mm;
                        }

                        html, body {
                            margin: 0;
                            padding: 0;
                            background: #f3f6fb;
                            color: #111827;
                            font-family: Arial, Helvetica, sans-serif;
                        }

                        .sheet {
                            background: #ffffff;
                            border-top: 10px solid #8b5cf6;
                            padding: 34px;
                        }

                        .top {
                            width: 100%%;
                            border-collapse: collapse;
                            margin-bottom: 28px;
                        }

                        .top td {
                            vertical-align: top;
                        }

                        .brand {
                            font-size: 30px;
                            font-weight: 700;
                            letter-spacing: 0.5px;
                            color: #111827;
                        }

                        .brand span {
                            color: #8b5cf6;
                        }

                        .subtitle {
                            font-size: 12px;
                            line-height: 1.6;
                            color: #6b7280;
                            margin-top: 8px;
                        }

                        .badge {
                            display: inline-block;
                            padding: 8px 14px;
                            border-radius: 999px;
                            background: #eef2ff;
                            color: #4f46e5;
                            font-size: 12px;
                            font-weight: 700;
                            text-transform: uppercase;
                            letter-spacing: 0.7px;
                        }

                        .meta {
                            font-size: 12px;
                            line-height: 1.7;
                            color: #374151;
                            margin-top: 12px;
                        }

                        .section {
                            margin-top: 18px;
                        }

                        .section-title {
                            font-size: 12px;
                            letter-spacing: 1px;
                            color: #6b7280;
                            font-weight: 700;
                            text-transform: uppercase;
                            margin-bottom: 12px;
                        }

                        .info-table,
                        .summary-table {
                            width: 100%;
                            border-collapse: collapse;
                        }

                        .info-table td {
                            padding: 11px 0;
                            border-bottom: 1px solid #e5e7eb;
                            font-size: 13px;
                        }

                        .label {
                            width: 34%;
                            color: #6b7280;
                        }

                        .value {
                            font-weight: 700;
                            color: #111827;
                        }

                        .summary {
                            margin-top: 26px;
                            background: #f9fafb;
                            border-radius: 18px;
                            padding: 24px;
                        }

                        .summary-table td {
                            padding: 10px 0;
                            font-size: 14px;
                        }

                        .summary-table .amount {
                            text-align: right;
                            font-size: 26px;
                            font-weight: 700;
                            color: #111827;
                        }

                        .total-row td {
                            border-top: 2px solid #111827;
                            padding-top: 16px;
                        }

                        .footer {
                            margin-top: 28px;
                            font-size: 11px;
                            line-height: 1.7;
                            color: #6b7280;
                        }
                    </style>
                </head>
                <body>
                    <div class="sheet">
                        <table class="top">
                            <tr>
                                <td>
                                    <div class="brand">Meet<span>Care</span></div>
                                    <div class="subtitle">
                                        Facture professionnelle generee depuis votre espace de suivi.
                                        <br />
                                        Template HTML/CSS optimise pour l'apercu et l'export PDF.
                                    </div>
                                </td>
                                <td style="text-align: right;">
                                    <div class="badge">Facture __STATUS_LABEL__</div>
                                    <div class="meta">
                                        Numero: __INVOICE_NUMBER__<br />
                                        Emise le: __ISSUE_DATE__<br />
                                        Echeance: __DUE_DATE__
                                    </div>
                                </td>
                            </tr>
                        </table>

                        <div class="section">
                            <div class="section-title">Informations</div>
                            <table class="info-table">
                                <tr>
                                    <td class="label">Medecin</td>
                                    <td class="value">__DOCTOR_NAME__</td>
                                </tr>
                                <tr>
                                    <td class="label">Specialite</td>
                                    <td class="value">__DOCTOR_SPECIALITY__</td>
                                </tr>
                                <tr>
                                    <td class="label">Patient</td>
                                    <td class="value">__PATIENT_NAME__</td>
                                </tr>
                                <tr>
                                    <td class="label">Statut</td>
                                    <td class="value">__STATUS_LABEL__</td>
                                </tr>
                                <tr>
                                    <td class="label">Paiement</td>
                                    <td class="value">__PAYMENT_METHOD__</td>
                                </tr>
                                <tr>
                                    <td class="label">Date de paiement</td>
                                    <td class="value">__PAID_AT__</td>
                                </tr>
                            </table>
                        </div>

                        <div class="summary">
                            <div class="section-title" style="margin-bottom: 16px;">Details de la prestation</div>
                            <table class="summary-table">
                                <tr>
                                    <td>__DESCRIPTION__</td>
                                    <td style="text-align: right; font-weight: 700;">__AMOUNT__</td>
                                </tr>
                                <tr class="total-row">
                                    <td>Total TTC</td>
                                    <td class="amount">__AMOUNT__</td>
                                </tr>
                            </table>
                        </div>

                        <div class="footer">
                            Document genere automatiquement par MeetCare.
                            Verification des informations recommandee avant impression ou archivage.
                        </div>
                    </div>
                </body>
                </html>
                """;

        return html
                .replace("__STATUS_LABEL__", escapeHtml(statusLabel))
                .replace("__INVOICE_NUMBER__", escapeHtml(invoiceNumber))
                .replace("__ISSUE_DATE__", escapeHtml(issueDate))
                .replace("__DUE_DATE__", escapeHtml(dueDate))
                .replace("__DOCTOR_NAME__", doctorName)
                .replace("__DOCTOR_SPECIALITY__", doctorSpeciality)
                .replace("__PATIENT_NAME__", patientName)
                .replace("__PAYMENT_METHOD__", paymentMethod)
                .replace("__PAID_AT__", escapeHtml(paidAt))
                .replace("__DESCRIPTION__", description)
                .replace("__AMOUNT__", amount);
    }

    private String buildInvoiceNumber(Invoice invoice) {
        int year = invoice.getCreatedAt() == null ? LocalDateTime.now().getYear() : invoice.getCreatedAt().getYear();
        int id = Math.max(invoice.getId(), 0);
        return String.format("MC-%d-%05d", year, id);
    }

    private String buildDefaultFileName(Invoice invoice) {
        return "facture_" + buildInvoiceNumber(invoice).toLowerCase(Locale.ROOT) + ".pdf";
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        DecimalFormat format = new DecimalFormat("0.00", symbols);
        return format.format(value) + " \u20AC";
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "Non renseignee" : DATE_FORMATTER.format(dateTime);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "Non renseignee" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void showInfo(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private void showError(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
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

    private String primaryActionStyle() {
        return "-fx-background-color: #EEF2FF; -fx-text-fill: #4F46E5; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #C7D2FE; "
                + "-fx-border-radius: 8; -fx-padding: 8 14 8 14;";
    }

    private String secondaryActionStyle() {
        return "-fx-background-color: white; -fx-text-fill: #444444; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #CCCCCC; "
                + "-fx-border-radius: 8; -fx-padding: 8 14 8 14;";
    }

    private Window resolveWindow(Object source) {
        if (source instanceof Node node && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return null;
    }
}
