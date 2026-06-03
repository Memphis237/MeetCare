package org.example.meetcare.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
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
            showError(owner, "Apercu indisponible", "Impossible d'ouvrir l'apercu.\n" + rootCauseMessage(ex));
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
        boolean paid = InvoiceStatus.PAID.equalsIgnoreCase(invoice.getStatus());
        String invoiceNumber = buildInvoiceNumber(invoice);
        String issueDate = formatDateTime(invoice.getCreatedAt());
        String paidDate = invoice.getPaidAt() == null ? "Non renseignee" : formatStampDateTime(invoice.getPaidAt());
        String doctorName = escapeHtml(valueOrDefault(invoice.getDoctorName(), "Medecin"));
        String doctorSpeciality = escapeHtml(valueOrDefault(invoice.getDoctorSpeciality(), "Specialite non renseignee"));
        String patientName = escapeHtml(valueOrDefault(invoice.getPatientName(), "Patient"));
        String description = escapeHtml(valueOrDefault(invoice.getDescription(), "Consultation"));

        BigDecimal totalAmount = invoice.getAmount() == null ? BigDecimal.ZERO : invoice.getAmount();
        BigDecimal baseAmount = totalAmount.divide(BigDecimal.valueOf(1.20), 2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = totalAmount.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);

        String amount = formatMoney(totalAmount);
        String baseAmountText = formatMoney(baseAmount);
        String vatAmountText = formatMoney(vatAmount);
        String statusLabel = paid ? "PAYÉ" : "EN ATTENTE";
        String stampLine1 = paid ? "Le : " + paidDate : "En attente de paiement";
        String stampLine2 = paid
                ? "Mode : " + valueOrDefault(invoice.getPaymentMethod(), "Non renseigne")
                : "Mode : -";

        String html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        @page {
                            size: A4;
                            margin: 14mm 14mm;
                        }

                        html, body {
                            margin: 0;
                            padding: 0;
                            background: #ffffff;
                            color: #111827;
                            font-family: Arial, Helvetica, sans-serif;
                        }

                        .page-bar {
                            position: absolute;
                            top: 0;
                            left: 0;
                            right: 0;
                            height: 6px;
                            background: #7c3aed;
                        }

                        .invoice {
                            position: relative;
                            background: #ffffff;
                            min-height: 269mm;
                            padding: 24px 26px 56mm 26px;
                            box-sizing: border-box;
                        }

                        .topline {
                            width: 100%;
                            border-collapse: collapse;
                            margin-bottom: 30px;
                        }

                        .topline td {
                            vertical-align: top;
                        }

                        .brand {
                            font-size: 31px;
                            font-weight: 700;
                            letter-spacing: -0.3px;
                            color: #1f2937;
                        }

                        .brand span {
                            color: #2f55f2;
                        }

                        .company {
                            margin-top: 12px;
                            font-size: 12px;
                            line-height: 1.6;
                            color: #5b6472;
                        }

                        .invoice-title {
                            font-size: 34px;
                            font-weight: 300;
                            color: #cbd5e1;
                            letter-spacing: 4px;
                            text-align: right;
                            margin-bottom: 22px;
                        }

                        .meta {
                            text-align: right;
                            font-size: 12px;
                            line-height: 1.7;
                            color: #6b7280;
                        }

                        .meta strong {
                            color: #4b5563;
                            font-weight: 700;
                        }

                        .grid {
                            display: table;
                            width: 100%;
                            margin-top: 10px;
                            margin-bottom: 8px;
                            table-layout: fixed;
                        }

                        .grid-cell {
                            display: table-cell;
                            width: 50%;
                            vertical-align: top;
                            padding-right: 18px;
                        }

                        .grid-cell.right {
                            padding-right: 0;
                            padding-left: 18px;
                        }

                        .block-label {
                            font-size: 12px;
                            font-weight: 700;
                            color: #2f55f2;
                            letter-spacing: 1.2px;
                            text-transform: uppercase;
                            margin-bottom: 8px;
                        }

                        .person-name {
                            font-size: 18px;
                            font-weight: 700;
                            color: #111827;
                            margin-bottom: 2px;
                        }

                        .person-sub {
                            font-size: 12px;
                            color: #374151;
                            line-height: 1.45;
                        }

                        .stamp-row {
                            width: 100%;
                            border-collapse: collapse;
                            margin: 2px 0 6px 0;
                        }

                        .stamp-row td {
                            vertical-align: top;
                        }

                        .stamp-row .spacer {
                            width: 65%;
                        }

                        .stamp {
                            display: inline-block;
                            padding: 12px 22px;
                            border-radius: 8px;
                            border: 4px solid #d1d5db;
                            color: #d1d5db;
                            font-size: 26px;
                            font-weight: 700;
                            transform: rotate(-12deg);
                            opacity: 0.9;
                            letter-spacing: 1px;
                            text-transform: uppercase;
                            background: rgba(255,255,255,0.6);
                        }

                        .stamp.paid {
                            border-color: #16a34a;
                            color: #16a34a;
                            background: rgba(255,255,255,0.85);
                        }

                        .stamp.pending {
                            border-color: #f7b8b8;
                            color: #f7b8b8;
                            opacity: 0.28;
                            background: rgba(255,255,255,0.18);
                        }

                        .stamp-lines {
                            margin-top: 6px;
                            font-size: 12px;
                            line-height: 1.35;
                            font-weight: 600;
                            text-transform: none;
                            letter-spacing: 0;
                        }

                        .items {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 28px;
                        }

                        .items thead th {
                            background: #eef2ff;
                            color: #54607b;
                            font-size: 12px;
                            text-transform: uppercase;
                            letter-spacing: 0.3px;
                            text-align: left;
                            padding: 10px 12px;
                        }

                        .items tbody td {
                            font-size: 13px;
                            padding: 12px 12px;
                            border-bottom: 1px solid #e5e7eb;
                            vertical-align: top;
                        }

                        .items .right {
                            text-align: right;
                            white-space: nowrap;
                        }

                        .items .desc strong {
                            font-weight: 700;
                            color: #111827;
                            display: block;
                            margin-bottom: 2px;
                        }

                        .items .desc span {
                            color: #64748b;
                            font-size: 12px;
                        }

                        .summary-wrap {
                            width: 100%;
                            margin-top: 24px;
                            display: table;
                        }

                        .summary-left {
                            display: table-cell;
                            width: 60%;
                        }

                        .summary-right {
                            display: table-cell;
                            width: 40%;
                            vertical-align: top;
                        }

                        .summary {
                            width: 100%;
                            border-collapse: collapse;
                        }

                        .summary td {
                            padding: 9px 0;
                            font-size: 14px;
                        }

                        .summary .label {
                            color: #6b7280;
                        }

                        .summary .value {
                            text-align: right;
                            font-size: 14px;
                            color: #111827;
                        }

                        .summary .net {
                            font-size: 16px;
                            font-weight: 700;
                            color: #2f55f2;
                        }

                        .footer {
                            position: absolute;
                            left: 26px;
                            right: 26px;
                            bottom: 18px;
                            padding-top: 12px;
                            border-top: 1px solid #edf0f6;
                            font-size: 11px;
                            line-height: 1.5;
                            color: #94a3b8;
                            text-align: center;
                        }
                    </style>
                </head>
                <body>
                    <div class="page-bar"></div>
                    <div class="invoice">
                        <div class="invoice-title">FACTURE</div>
                        <table class="topline">
                            <tr>
                                <td>
                                    <div class="brand">Meet<span>Care</span></div>
                                    <div class="company">
                                        MeetCare Medical Solutions SAS<br />
                                        85 Avenue de la Republique<br />
                                        75011 Paris, France<br />
                                        SIRET : 882 341 992 00014
                                    </div>
                                </td>
                                <td style="text-align: right;">
                                    <div class="meta">
                                        <strong>REFERENCE</strong> __INVOICE_NUMBER__<br />
                                        <strong>DATE D'EMISSION</strong> __ISSUE_DATE__<br />
                                        <strong>CODE CLIENT</strong> __CLIENT_CODE__
                                    </div>
                                </td>
                            </tr>
                        </table>

                        <div class="grid">
                            <div class="grid-cell">
                                <div class="block-label">EMETTEUR</div>
                                <div class="person-name">Dr. __DOCTOR_NAME__</div>
                                <div class="person-sub">
                                    Specialite : __DOCTOR_SPECIALITY__<br />
                                    RPPS : 10100982736
                                </div>
                            </div>
                            <div class="grid-cell right">
                                <div class="block-label">FACTURER A</div>
                                <div class="person-name">__PATIENT_NAME__</div>
                                <div class="person-sub">
                                    Patient de MeetCare<br />
                                    Consultation medicale
                                </div>
                            </div>
                        </div>

                        <table class="stamp-row">
                            <tr>
                                <td class="spacer"></td>
                                <td style="text-align: right;">
                                    <div class="stamp __STAMP_CLASS__">
                                        __STAMP_TITLE__
                                        <div class="stamp-lines">__STAMP_LINE_1__<br />__STAMP_LINE_2__</div>
                                    </div>
                                </td>
                            </tr>
                        </table>

                        <table class="items">
                            <thead>
                                <tr>
                                    <th>DESCRIPTION DE LA PRESTATION</th>
                                    <th class="right">PRIX UNITAIRE HT</th>
                                    <th class="right">TVA</th>
                                    <th class="right">TOTAL HT</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td class="desc">
                                        <strong>__DESCRIPTION__</strong>
                                        <span>Code acte : Consultation standard</span>
                                    </td>
                                    <td class="right">__BASE_AMOUNT__</td>
                                    <td class="right">20%</td>
                                    <td class="right">__BASE_AMOUNT__</td>
                                </tr>
                            </tbody>
                        </table>

                        <div class="summary-wrap">
                            <div class="summary-left"></div>
                            <div class="summary-right">
                                <table class="summary">
                                    <tr>
                                        <td class="label">Total Partiel HT</td>
                                        <td class="value">__BASE_AMOUNT__</td>
                                    </tr>
                                    <tr>
                                        <td class="label">TVA (20%)</td>
                                        <td class="value">__VAT_AMOUNT__</td>
                                    </tr>
                                    <tr>
                                        <td class="label net">NET A PAYER</td>
                                        <td class="value net">__AMOUNT__</td>
                                    </tr>
                                </table>
                            </div>
                        </div>

                        <div class="footer">
                            MeetCare Solutions SAS au capital de 50 000 € - RCS Paris B 882 341 992 - TVA intracommunautaire FR 12 882341992<br />
                            En cas de retard de paiement, une indemnite forfaitaire de 40 € sera due (Art. L 441-6 du Code de Commerce).<br />
                            Merci de votre confiance.
                        </div>
                    </div>
                </body>
                </html>
                """;

        return html
                .replace("__STAMP_CLASS__", paid ? "paid" : "pending")
                .replace("__STAMP_TITLE__", escapeHtml(statusLabel))
                .replace("__STAMP_LINE_1__", escapeHtml(stampLine1))
                .replace("__STAMP_LINE_2__", escapeHtml(stampLine2))
                .replace("__INVOICE_NUMBER__", escapeHtml(invoiceNumber))
                .replace("__ISSUE_DATE__", escapeHtml(issueDate))
                .replace("__CLIENT_CODE__", escapeHtml(buildClientCode(invoice)))
                .replace("__DOCTOR_NAME__", doctorName)
                .replace("__DOCTOR_SPECIALITY__", doctorSpeciality)
                .replace("__PATIENT_NAME__", patientName)
                .replace("__DESCRIPTION__", description)
                .replace("__BASE_AMOUNT__", baseAmountText)
                .replace("__VAT_AMOUNT__", vatAmountText)
                .replace("__AMOUNT__", amount);
    }

    private String buildInvoiceNumber(Invoice invoice) {
        int year = invoice.getCreatedAt() == null ? LocalDateTime.now().getYear() : invoice.getCreatedAt().getYear();
        int id = Math.max(invoice.getId(), 0);
        return String.format("INV-%d-%05d", year, id);
    }

    private String buildClientCode(Invoice invoice) {
        int id = Math.max(invoice.getPatientId(), 0);
        int year = invoice.getCreatedAt() == null ? LocalDateTime.now().getYear() : invoice.getCreatedAt().getYear();
        return String.format("CL-%d-%04d", year, id);
    }

    private String buildDefaultFileName(Invoice invoice) {
        return "facture_" + buildInvoiceNumber(invoice).toLowerCase(Locale.ROOT) + ".pdf";
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        DecimalFormat format = new DecimalFormat("0.00", symbols);
        return format.format(value) + " €";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "Non renseignee" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatStampDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "Non renseignee" : dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));
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
