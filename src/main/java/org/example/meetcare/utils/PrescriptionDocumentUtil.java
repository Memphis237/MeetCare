package org.example.meetcare.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrescriptionDocumentUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void renderPrescription(PrescriptionRequest request, Path outputPath) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(buildHtml(request), outputPath.getParent() == null
                    ? Path.of("").toAbsolutePath().toUri().toString()
                    : outputPath.getParent().toUri().toString());
            builder.toStream(outputStream);
            builder.run();
        }
    }

    public String buildDefaultFileName(String patientName, LocalDateTime dateTime) {
        String safePatient = sanitize(patientName == null || patientName.isBlank() ? "patient" : patientName);
        String stamp = (dateTime == null ? LocalDateTime.now() : dateTime).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        return "ordonnance_" + safePatient + "_" + stamp + ".pdf";
    }

    public String buildHtml(PrescriptionRequest request) {
        LocalDateTime createdAt = request.createdAt() == null ? LocalDateTime.now() : request.createdAt();
        String issueDate = DATE_FORMATTER.format(createdAt);
        String issueStamp = createdAt.format(DateTimeFormatter.ofPattern("ddMMyyyyHHmm"));

        String doctorName = escapeHtml(valueOrDefault(request.doctorName(), "Medecin"));
        String doctorSpeciality = escapeHtml(valueOrDefault(request.doctorSpeciality(), "Medecine generale"));
        String patientName = escapeHtml(valueOrDefault(request.patientName(), "Patient"));
        String diagnosis = escapeHtml(valueOrDefault(request.diagnosis(), "Consultation medicale"));
        String validity = escapeHtml(valueOrDefault(request.validity(), "3 mois"));

        String treatmentRows = buildTreatmentRows(parseTreatmentItems(valueOrDefault(request.treatment(), "")));
        String instructionsRows = buildInstructionItems(valueOrDefault(request.instructions(), ""));

        String html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        @page {
                            size: A4;
                            margin: 0;
                        }

                        html, body {
                            margin: 0;
                            padding: 0;
                            background: #ffffff;
                            color: #1f2937;
                            font-family: Arial, Helvetica, sans-serif;
                        }

                        .page {
                            min-height: 100vh;
                            position: relative;
                            box-sizing: border-box;
                            padding-bottom: 42px;
                        }

                        .topbar {
                            background: #1e5aa8;
                            color: #ffffff;
                            padding: 16px 38px;
                            display: table;
                            width: 100%%;
                            box-sizing: border-box;
                        }

                        .brand {
                            display: table-cell;
                            vertical-align: middle;
                        }

                        .brand-title {
                            font-size: 26px;
                            font-weight: 800;
                            letter-spacing: -0.4px;
                        }

                        .brand-subtitle {
                            font-size: 12px;
                            margin-top: 6px;
                            opacity: 0.92;
                        }

                        .badge-wrap {
                            display: table-cell;
                            vertical-align: middle;
                            text-align: right;
                        }

                        .badge {
                            display: inline-block;
                            min-width: 170px;
                            padding: 16px 18px;
                            border-radius: 8px;
                            background: #ffffff;
                            color: #1e5aa8;
                            font-weight: 800;
                            font-size: 14px;
                            letter-spacing: 0.5px;
                            text-transform: uppercase;
                            text-align: center;
                        }

                        .meta-row {
                            background: #eef4fb;
                            padding: 12px 40px;
                            display: table;
                            width: 100%%;
                            box-sizing: border-box;
                            border-bottom: 1px solid #d8e3f2;
                        }

                        .meta-item {
                            display: table-cell;
                            width: 33.333%%;
                            font-size: 12px;
                            color: #41526b;
                        }

                        .content {
                            padding: 22px 40px 18px 40px;
                        }

                        .two-cols {
                            display: table;
                            width: 100%%;
                            border-spacing: 14px 0;
                            table-layout: fixed;
                            margin-top: 16px;
                        }

                        .col {
                            display: table-cell;
                            width: 50%%;
                            vertical-align: top;
                        }

                        .card {
                            border: 1px solid #d7dfea;
                            border-radius: 6px;
                            overflow: hidden;
                            background: #ffffff;
                        }

                        .card-head {
                            background: #eef4fb;
                            color: #1d4d93;
                            font-size: 12px;
                            font-weight: 800;
                            text-transform: uppercase;
                            padding: 8px 12px;
                            border-bottom: 1px solid #d7dfea;
                        }

                        .card-body {
                            padding: 12px;
                        }

                        .person-name {
                            font-size: 16px;
                            font-weight: 800;
                            color: #13233b;
                            margin-bottom: 5px;
                        }

                        .person-line {
                            font-size: 12px;
                            line-height: 1.55;
                            color: #5b6a80;
                        }

                        .shared-note {
                            margin-top: 8px;
                            font-size: 11px;
                            color: #16a34a;
                            font-weight: 700;
                            text-align: right;
                        }

                        .section {
                            margin-top: 34px;
                        }

                        .section-title {
                            display: table;
                            width: 100%%;
                            border-collapse: collapse;
                            margin-bottom: 6px;
                        }

                        .section-title .bar {
                            width: 8px;
                            background: #1e5aa8;
                        }

                        .section-title .label {
                            padding-left: 8px;
                            font-size: 13px;
                            font-weight: 800;
                            color: #13233b;
                            text-transform: uppercase;
                            letter-spacing: 0.3px;
                            border-bottom: 1px solid #cbd5e1;
                        }

                        .panel {
                            border: 1px solid #d7dfea;
                            border-radius: 4px;
                            background: #f7f9fc;
                            padding: 12px;
                            font-size: 13px;
                            color: #2f3c52;
                            line-height: 1.65;
                        }

                        .treatment-table {
                            width: 100%%;
                            border-collapse: collapse;
                            font-size: 12px;
                        }

                        .treatment-table th {
                            background: #eef4fb;
                            border-bottom: 1px solid #d7dfea;
                            color: #1d4d93;
                            text-align: left;
                            padding: 8px 10px;
                            font-size: 11px;
                            text-transform: uppercase;
                            letter-spacing: 0.3px;
                        }

                        .treatment-table td {
                            border-bottom: 1px solid #d7dfea;
                            padding: 11px 10px;
                            vertical-align: top;
                            color: #253247;
                            background: #ffffff;
                        }

                        .num-badge {
                            display: inline-block;
                            width: 28px;
                            height: 28px;
                            line-height: 28px;
                            border-radius: 999px;
                            background: #1e5aa8;
                            color: #ffffff;
                            text-align: center;
                            font-weight: 800;
                            font-size: 12px;
                        }

                        .med-name {
                            font-weight: 800;
                            margin-bottom: 4px;
                            font-size: 13px;
                        }

                        .med-note {
                            color: #5b6a80;
                            font-size: 11px;
                            line-height: 1.5;
                        }

                        .instructions-list {
                            margin: 0;
                            padding-left: 16px;
                        }

                        .instructions-list li {
                            margin-bottom: 4px;
                        }

                        .signature-box {
                            width: 240px;
                            margin-left: auto;
                            margin-top: 10px;
                            border: 1px solid #d7dfea;
                            border-radius: 6px;
                            padding: 10px 12px;
                            background: #ffffff;
                        }

                        .signature-label {
                            font-size: 11px;
                            color: #5b6a80;
                            margin-bottom: 8px;
                        }

                        .signature-name {
                            font-size: 18px;
                            font-style: italic;
                            color: #1e5aa8;
                            font-weight: 400;
                            margin-bottom: 4px;
                        }

                        .signature-meta {
                            font-size: 11px;
                            color: #5b6a80;
                            line-height: 1.45;
                        }

                        .footer {
                            position: absolute;
                            left: 0;
                            right: 0;
                            bottom: 0;
                            background: #1e5aa8;
                            color: white;
                            font-size: 10px;
                            padding: 8px 18px;
                            text-align: center;
                            line-height: 1.4;
                            box-sizing: border-box;
                        }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="topbar">
                            <div class="brand">
                                <div class="brand-title">+ MeetCare</div>
                                <div class="brand-subtitle">Plateforme de sante numerique</div>
                            </div>
                            <div class="badge-wrap">
                                <div class="badge">Ordonnance medicale</div>
                            </div>
                        </div>

                        <div class="meta-row">
                            <div class="meta-item"><strong>Ref. :</strong> ORD-__ISSUE_STAMP__</div>
                            <div class="meta-item"><strong>Date d'emission :</strong> __ISSUE_DATE__</div>
                            <div class="meta-item"><strong>Validite :</strong> __VALIDITY__</div>
                        </div>

                        <div class="content">
                            <div class="two-cols">
                                <div class="col">
                                    <div class="card">
                                        <div class="card-head">Prescripteur</div>
                                        <div class="card-body">
                                            <div class="person-name">Dr. __DOCTOR_NAME__</div>
                                            <div class="person-line">__DOCTOR_SPECIALITY__</div>
                                            <div class="person-line">Cabinet MeetCare</div>
                                            <div class="person-line">Plateforme MeetCare</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="col">
                                    <div class="card">
                                        <div class="card-head">Patient</div>
                                        <div class="card-body">
                                            <div class="person-name">__PATIENT_NAME__</div>
                                            <div class="person-line">Document partage au patient</div>
                                            <div class="shared-note">Document partage avec le patient</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="section">
                                <div class="section-title">
                                    <div class="bar"></div>
                                    <div class="label">Motif de consultation / diagnostic</div>
                                </div>
                                <div class="panel">__DIAGNOSIS__</div>
                            </div>

                            <div class="section">
                                <div class="section-title">
                                    <div class="bar"></div>
                                    <div class="label">Traitement prescrit</div>
                                </div>
                                <table class="treatment-table">
                                    <thead>
                                        <tr>
                                            <th style="width: 44px;">N</th>
                                            <th>Medicament / Prestation</th>
                                            <th>Posologie / Instructions</th>
                                            <th style="width: 90px;">Duree</th>
                                            <th style="width: 140px;">Remarques</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        __TREATMENT_ROWS__
                                    </tbody>
                                </table>
                            </div>

                            <div class="section">
                                <div class="section-title">
                                    <div class="bar"></div>
                                    <div class="label">Recommandations et instructions</div>
                                </div>
                                <div class="panel">
                                    <ul class="instructions-list">
                                        __INSTRUCTION_ITEMS__
                                    </ul>
                                </div>
                            </div>

                            <div class="signature-box">
                                <div class="signature-label">signature du medecin</div>
                                <div class="signature-name">Dr. __DOCTOR_NAME__</div>
                                <div class="signature-meta">__DOCTOR_SPECIALITY__</div>
                                <div class="signature-meta">MeetCare</div>
                            </div>
                        </div>

                        <div class="footer">
                            MeetCare - Plateforme de sante numerique - meetcare.fr - Document genere le __ISSUE_DATE__ - Ref. ORD-__ISSUE_STAMP__ | Conservez cette ordonnance pour votre suivi medical
                        </div>
                    </div>
                </body>
                </html>
                """;

        return html
                .replace("__DOCTOR_NAME__", doctorName)
                .replace("__DOCTOR_SPECIALITY__", doctorSpeciality)
                .replace("__PATIENT_NAME__", patientName)
                .replace("__DIAGNOSIS__", diagnosis)
                .replace("__TREATMENT_ROWS__", treatmentRows)
                .replace("__INSTRUCTION_ITEMS__", instructionsRows)
                .replace("__VALIDITY__", validity)
                .replace("__ISSUE_DATE__", escapeHtml(issueDate))
                .replace("__ISSUE_STAMP__", escapeHtml(issueStamp));
    }

    private String buildTreatmentRows(List<TreatmentItem> items) {
        if (items.isEmpty()) {
            return """
                    <tr>
                        <td><span class="num-badge">1</span></td>
                        <td><div class="med-name">Aucun traitement renseigne</div></td>
                        <td></td>
                        <td></td>
                        <td></td>
                    </tr>
                    """;
        }

        StringBuilder rows = new StringBuilder();
        for (int index = 0; index < items.size(); index++) {
            TreatmentItem item = items.get(index);
            rows.append("""
                    <tr>
                        <td><span class="num-badge">%d</span></td>
                        <td>
                            <div class="med-name">%s</div>
                        </td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                    </tr>
                    """.formatted(
                    index + 1,
                    escapeHtml(item.name()),
                    escapeHtml(item.instructions()),
                    escapeHtml(item.duration()),
                    escapeHtml(item.remarks())
            ));
        }
        return rows.toString();
    }

    private String buildInstructionItems(String instructions) {
        List<String> items = splitLines(instructions);
        if (items.isEmpty()) {
            items = List.of("Suivre les recommandations medicales.");
        }

        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            builder.append("<li>").append(escapeHtml(item)).append("</li>");
        }
        return builder.toString();
    }

    private List<TreatmentItem> parseTreatmentItems(String treatmentText) {
        List<String> lines = splitLines(treatmentText);
        List<TreatmentItem> items = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split("\\|", -1);
            String name = trimPart(parts, 0);
            if (name.isBlank()) {
                continue;
            }
            String instructions = trimPart(parts, 1);
            String duration = trimPart(parts, 2);
            String remarks = trimPart(parts, 3);
            items.add(new TreatmentItem(name, instructions, duration, remarks));
        }
        return items;
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = normalized.split("\n");
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("-")) {
                line = line.substring(1).trim();
            }
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private String trimPart(String[] parts, int index) {
        if (index >= parts.length || parts[index] == null) {
            return "";
        }
        return parts[index].trim();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String sanitize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\\\/:*?\"<>|\\s]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
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

    public record PrescriptionRequest(
            String doctorName,
            String doctorSpeciality,
            String patientName,
            String diagnosis,
            String treatment,
            String instructions,
            String validity,
            LocalDateTime createdAt
    ) {
    }

    private record TreatmentItem(String name, String instructions, String duration, String remarks) {
    }
}
