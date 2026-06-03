package org.example.meetcare.utils;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;

public final class DocumentPreviewUtil {
    private DocumentPreviewUtil() {
    }

    public static void showPreview(Window owner, String filePath, String displayName) {
        File file = filePath == null ? null : new File(filePath);
        if (file == null || !file.exists()) {
            showError("Document introuvable", "Le fichier demande est introuvable.");
            return;
        }

        try {
            String html = buildPreviewHtml(file, displayName);
            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setTitle("Prévisualisation - " + safeTitle(displayName, file.getName()));

            WebView webView = new WebView();
            webView.getEngine().loadContent(html);

            Label title = new Label(safeTitle(displayName, file.getName()));
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label subtitle = new Label(file.getAbsolutePath());
            subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
            subtitle.setWrapText(true);

            VBox topText = new VBox(2, title, subtitle);

            Button close = new Button("Fermer");
            close.setStyle("-fx-background-color: #1D4ED8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 14 8 14;");
            close.setOnAction(event -> stage.close());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox topBar = new HBox(12, topText, spacer, close);
            topBar.setPadding(new Insets(14));
            topBar.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;");

            BorderPane root = new BorderPane(webView);
            root.setTop(topBar);
            root.setStyle("-fx-background-color: #F3F4F6;");

            Scene scene = new Scene(root, 1100, 780);
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible d'ouvrir l'aperçu", rootCauseMessage(ex));
        }
    }

    private static String buildPreviewHtml(File file, String displayName) throws Exception {
        String lower = file.getName().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        if (isImage(lower)) {
            return wrapSingleImage(file.toURI().toString(), displayName);
        }
        if (lower.endsWith(".pdf")) {
            return renderPdfToHtml(file, displayName);
        }
        if (lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".csv")) {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return wrapTextPreview(content, displayName);
        }
        throw new IllegalArgumentException("Format de document non supporte: " + file.getName());
    }

    private static String renderPdfToHtml(File file, String displayName) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            padding: 24px;
                            background: #F3F4F6;
                            font-family: Arial, sans-serif;
                        }
                        .page {
                            margin: 0 auto 24px auto;
                            max-width: 1000px;
                            background: white;
                            border: 1px solid #E5E7EB;
                            box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
                        }
                        img {
                            width: 100%%;
                            display: block;
                        }
                    </style>
                </head>
                <body>
                """.formatted(escapeHtml(displayName)));

        try (PDDocument document = PDDocument.load(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 144, ImageType.RGB);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
                html.append("<div class=\"page\"><img alt=\"page ")
                        .append(pageIndex + 1)
                        .append("\" src=\"data:image/png;base64,")
                        .append(base64)
                        .append("\" /></div>");
            }
        }

        html.append("</body></html>");
        return html.toString();
    }

    private static String wrapSingleImage(String imageUrl, String displayName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            padding: 24px;
                            background: #F3F4F6;
                            font-family: Arial, sans-serif;
                        }
                        .frame {
                            max-width: 1100px;
                            margin: 0 auto;
                            background: white;
                            border: 1px solid #E5E7EB;
                            box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
                            padding: 20px;
                        }
                        img {
                            width: 100%%;
                            height: auto;
                            display: block;
                        }
                    </style>
                </head>
                <body>
                    <div class="frame">
                        <img src="%s" alt="%s" />
                    </div>
                </body>
                </html>
                """.formatted(escapeHtml(displayName), imageUrl, escapeHtml(displayName));
    }

    private static String wrapTextPreview(String content, String displayName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            padding: 24px;
                            background: #F3F4F6;
                            font-family: Arial, sans-serif;
                        }
                        pre {
                            white-space: pre-wrap;
                            word-break: break-word;
                            background: white;
                            border: 1px solid #E5E7EB;
                            box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
                            padding: 20px;
                        }
                    </style>
                </head>
                <body>
                    <pre>%s</pre>
                </body>
                </html>
                """.formatted(escapeHtml(displayName), escapeHtml(content));
    }

    private static boolean isImage(String lowerFileName) {
        return lowerFileName.endsWith(".png")
                || lowerFileName.endsWith(".jpg")
                || lowerFileName.endsWith(".jpeg")
                || lowerFileName.endsWith(".webp")
                || lowerFileName.endsWith(".gif")
                || lowerFileName.endsWith(".bmp");
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String safeTitle(String displayName, String fallback) {
        if (displayName == null || displayName.isBlank()) {
            return fallback;
        }
        return displayName;
    }

    private static String escapeHtml(String value) {
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

    private static String rootCauseMessage(Throwable throwable) {
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
