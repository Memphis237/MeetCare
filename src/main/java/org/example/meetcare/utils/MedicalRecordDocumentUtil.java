package org.example.meetcare.utils;

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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.example.meetcare.model.MedicalRecord;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class MedicalRecordDocumentUtil {

    public Button createConsultButton(MedicalRecord record) {
        Button button = new Button("Consulter");
        button.setStyle(primaryStyle());
        button.setOnAction(event -> showPreview(record, resolveWindow(event.getSource())));
        return button;
    }

    public Button createDownloadButton(MedicalRecord record) {
        Button button = new Button("Télécharger");
        button.setStyle(secondaryStyle());
        button.setOnAction(event -> downloadFile(record, resolveWindow(event.getSource())));
        return button;
    }

    public void showPreview(MedicalRecord record, Window owner) {
        try {
            Path path = Path.of(record.getFilePath());
            if (!Files.exists(path)) {
                showError(owner, "Fichier introuvable", "Le fichier source n'existe pas.");
                return;
            }

            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.getEngine().loadContent(buildPreviewHtml(record, path));
            webView.setPrefSize(960, 680);

            VBox content = new VBox(webView);
            content.setStyle("-fx-background-color: #F3F6FB; -fx-padding: 0;");

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Aperçu du document");
            dialog.setHeaderText(record.getTitle());
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
            showError(owner, "Erreur d'aperçu", rootCauseMessage(ex));
            ex.printStackTrace();
        }
    }

    public void downloadFile(MedicalRecord record, Window owner) {
        try {
            Path source = Path.of(record.getFilePath());
            if (!Files.exists(source)) {
                showError(owner, "Fichier introuvable", "Le fichier source n'existe pas.");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer le document");
            chooser.setInitialFileName(defaultFileName(record, source));
            File target = chooser.showSaveDialog(owner);
            if (target == null) {
                return;
            }

            Files.copy(source, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            showInfo(owner, "Téléchargé", "Fichier enregistré dans: " + target.getAbsolutePath());
        } catch (Exception ex) {
            showError(owner, "Erreur de téléchargement", rootCauseMessage(ex));
            ex.printStackTrace();
        }
    }

    private String buildPreviewHtml(MedicalRecord record, Path path) throws Exception {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        String displayName = escapeHtml(record.getTitle() == null || record.getTitle().isBlank()
                ? path.getFileName().toString()
                : record.getTitle());
        String documentType = escapeHtml(record.getDocumentType() == null ? "OTHER" : record.getDocumentType());
        String description = escapeHtml(record.getDescription() == null ? "" : record.getDescription());

        String fileContent;
        if (lower.endsWith(".pdf")) {
            fileContent = renderPdfToHtml(path);
        } else if (isImage(lower)) {
            fileContent = "<img src=\"" + path.toUri() + "\" style=\"max-width:100%;height:auto;display:block;margin:0 auto;\" />";
        } else if (lower.endsWith(".txt") || lower.endsWith(".csv") || lower.endsWith(".log")) {
            String content = escapeHtml(Files.readString(path));
            fileContent = "<pre style=\"white-space:pre-wrap;word-break:break-word;background:white;border:1px solid #E5E7EB;padding:18px;\">" + content + "</pre>";
        } else {
            fileContent = "<div style=\"padding:18px;background:white;border:1px solid #E5E7EB;\">"
                    + "<h3 style=\"margin-top:0;\">Aperçu non disponible</h3>"
                    + "<p>Ce format ne peut pas être affiché directement. Utilisez le bouton Télécharger.</p>"
                    + "</div>";
        }

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            background: #F3F6FB;
                            font-family: Arial, Helvetica, sans-serif;
                            color: #111827;
                        }
                        .wrap {
                            padding: 24px;
                        }
                        .card {
                            background: white;
                            border: 1px solid #E5E7EB;
                            border-radius: 16px;
                            box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
                            overflow: hidden;
                        }
                        .header {
                            padding: 18px 20px;
                            border-bottom: 1px solid #EEF2F7;
                            display: flex;
                            gap: 16px;
                            align-items: flex-start;
                            justify-content: space-between;
                        }
                        .title {
                            font-size: 20px;
                            font-weight: 700;
                            margin: 0;
                        }
                        .meta {
                            font-size: 13px;
                            color: #6B7280;
                            margin-top: 6px;
                            line-height: 1.6;
                        }
                        .badge {
                            display: inline-block;
                            margin-top: 4px;
                            padding: 6px 10px;
                            border-radius: 999px;
                            font-size: 12px;
                            font-weight: 700;
                            color: #1D4ED8;
                            background: #EFF6FF;
                        }
                        .desc {
                            margin-top: 10px;
                            padding-top: 10px;
                            border-top: 1px solid #EEF2F7;
                            font-size: 13px;
                            color: #4B5563;
                            white-space: pre-wrap;
                        }
                        .viewer {
                            min-height: 560px;
                            background: #F8FAFC;
                        }
                    </style>
                </head>
                <body>
                    <div class="wrap">
                        <div class="card">
                            <div class="header">
                                <div>
                                    <h1 class="title">%s</h1>
                                    <div class="badge">%s</div>
                                    <div class="meta">Fichier: %s</div>
                                </div>
                            </div>
                            <div class="header" style="display:block;">
                                <div class="meta">Description</div>
                                <div class="desc">%s</div>
                            </div>
                            <div class="viewer">%s</div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(displayName, displayName, documentType, escapeHtml(path.getFileName().toString()), description, fileContent);
    }

    private boolean isImage(String lowerFileName) {
        return lowerFileName.endsWith(".png")
                || lowerFileName.endsWith(".jpg")
                || lowerFileName.endsWith(".jpeg")
                || lowerFileName.endsWith(".webp")
                || lowerFileName.endsWith(".gif")
                || lowerFileName.endsWith(".bmp");
    }

    private String renderPdfToHtml(Path path) throws Exception {
        StringBuilder html = new StringBuilder();
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 150, ImageType.RGB);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
                html.append("<div style=\"padding:18px;border-bottom:1px solid #E5E7EB;background:white;\">")
                        .append("<img alt=\"page ").append(pageIndex + 1)
                        .append("\" src=\"data:image/png;base64,")
                        .append(base64)
                        .append("\" style=\"width:100%;height:auto;display:block;\"/>")
                        .append("</div>");
            }
        }

        return "<div style=\"padding:16px;\">"
                + "<div style=\"margin-bottom:12px;font-size:13px;color:#6B7280;\">"
                + "Apercu PDF"
                + "</div>"
                + html
                + "</div>";
    }

    private String defaultFileName(MedicalRecord record, Path source) {
        String base = record.getTitle() == null || record.getTitle().isBlank()
                ? source.getFileName().toString()
                : record.getTitle().trim();
        String safe = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        String extension = extractExtension(source.getFileName().toString());
        return safe + extension;
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index);
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

    private Window resolveWindow(Object source) {
        if (source instanceof Node node && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return null;
    }

    private String primaryStyle() {
        return "-fx-background-color: #EEF2FF; -fx-text-fill: #2563eb; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 14 8 14;";
    }

    private String secondaryStyle() {
        return "-fx-background-color: white; -fx-text-fill: #444444; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: #CCCCCC; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 14 8 14;";
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

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
