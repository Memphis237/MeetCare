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
                            width: 100%%;
                            height: 100%%;
                            margin: 0;
                            padding: 0;
                            background: #ffffff;
                            color: #1f2937;
                            font-family: Arial, Helvetica, sans-serif;
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }

                        .page {
                            width: 100%%;
                            min-height: 297mm;
                            position: relative;
                            box-sizing: border-box;
                            padding-bottom: 16mm;
                            overflow: hidden;
                        }

                        .topbar {
                            width: 100%%;
                            background: #1f5ba7;
                            color: #ffffff;
                            box-sizing: border-box;
                            padding: 7.5mm 7mm 8mm 7mm;
                        }

                        .topbar-table {
                            width: 100%%;
                            border-collapse: collapse;
                            table-layout: fixed;
                        }

                        .topbar-table td {
                            vertical-align: middle;
                        }

                        .brand-cell {
                            width: 60%%;
                        }

                        .brand-title {
                            font-size: 24px;
                            line-height: 1;
                            font-weight: 800;
                            letter-spacing: -0.6px;
                        }

                        .brand-subtitle {
                            font-size: 9.5px;
                            margin-top: 7px;
                            line-height: 1.2;
                            opacity: 0.96;
                        }

                        .badge-cell {
                            text-align: right;
                            width: 40%%;
                            padding-right: 2mm;
                        }

                        .badge {
                            display: inline-block;
                            min-width: 49mm;
                            padding: 7px 11px;
                            border-radius: 5px;
                            background: #ffffff;
                            color: #1f5ba7;
                            font-weight: 800;
                            font-size: 12px;
                            text-transform: uppercase;
                            letter-spacing: 0.2px;
                            text-align: center;
                        }

                        .meta-row {
                            width: 100%%;
                            background: #eef4fb;
                            border-bottom: 1px solid #d8e3f2;
                            box-sizing: border-box;
                            padding: 7px 8mm;
                        }

                        .meta-table {
                            width: 100%%;
                            border-collapse: collapse;
                            table-layout: fixed;
                        }

                        .meta-table td {
                            font-size: 10.5px;
                            color: #475569;
                            padding: 0;
                            vertical-align: middle;
                        }

                        .body {
                            padding: 10mm 7mm 0 7mm;
                        }

                        .two-cols {
                            width: 100%%;
                            border-collapse: separate;
                            border-spacing: 4.5mm 0;
                            table-layout: fixed;
                        }

                        .two-cols td {
                            width: 50%%;
                            vertical-align: top;
                        }

                        .card {
                            border: 1px solid #d7dfea;
                            border-radius: 5px;
                            overflow: hidden;
                            background: #ffffff;
                        }

                        .card-head {
                            background: #eef4fb;
                            color: #1d4d93;
                            font-size: 10px;
                            font-weight: 800;
                            text-transform: uppercase;
                            padding: 6px 10px;
                            border-bottom: 1px solid #d7dfea;
                        }

                        .card-body {
                            padding: 8px 10px 9px 10px;
                        }

                        .person-name {
                            font-size: 13.5px;
                            line-height: 1.2;
                            font-weight: 800;
                            color: #13233b;
                            margin-bottom: 4px;
                        }

                        .person-line {
                            font-size: 10.5px;
                            line-height: 1.4;
                            color: #5b6a80;
                        }

                        .shared-note {
                            margin-top: 4px;
                            font-size: 10.5px;
                            color: #16a34a;
                            font-weight: 700;
                            text-align: right;
                        }

                        .section {
                            margin-top: 13.5mm;
                            page-break-inside: avoid;
                        }

                        .section-title {
                            display: table;
                            width: 100%%;
                            border-collapse: collapse;
                            margin-bottom: 3px;
                        }

                        .section-title .bar {
                            width: 3mm;
                            background: #1f5ba7;
                        }

                        .section-title .label {
                            padding-left: 3px;
                            font-size: 10.5px;
                            font-weight: 800;
                            color: #13233b;
                            text-transform: uppercase;
                            letter-spacing: 0.1px;
                            border-bottom: 1px solid #cbd5e1;
                            line-height: 1.1;
                        }

                        .panel {
                            border: 1px solid #d7dfea;
                            border-radius: 3px;
                            background: #f7f9fc;
                            padding: 9px 12px;
                            font-size: 10.5px;
                            color: #2f3c52;
                            line-height: 1.4;
                        }

                        .treatment-table {
                            width: 100%%;
                            border-collapse: collapse;
                            font-size: 10.5px;
                        }

                        .treatment-table th {
                            background: #eef4fb;
                            border-bottom: 1px solid #d7dfea;
                            color: #1d4d93;
                            text-align: left;
                            padding: 5px 6px;
                            font-size: 9.5px;
                            text-transform: uppercase;
                            letter-spacing: 0.25px;
                        }

                        .treatment-table td {
                            border-bottom: 1px solid #d7dfea;
                            padding: 8px 6px;
                            vertical-align: top;
                            color: #253247;
                            background: #ffffff;
                            line-height: 1.35;
                        }

                        .num-badge {
                            display: inline-block;
                            width: 7.5mm;
                            height: 7.5mm;
                            line-height: 7.5mm;
                            border-radius: 999px;
                            background: #2459d3;
                            color: #ffffff;
                            text-align: center;
                            font-weight: 800;
                            font-size: 11px;
                        }

                        .med-name {
                            font-weight: 800;
                            margin-bottom: 3px;
                            font-size: 11.5px;
                        }

                        .instructions-list {
                            margin: 0;
                            padding-left: 14px;
                        }

                        .instructions-list li {
                            margin-bottom: 3px;
                        }

                        .signature-box {
                            width: 60mm;
                            margin-left: auto;
                            margin-top: 2.5mm;
                            border: 1px solid #d7dfea;
                            border-radius: 4px;
                            padding: 8px 10px;
                            background: #ffffff;
                        }

                        .signature-label {
                            font-size: 9.5px;
                            color: #5b6a80;
                            margin-bottom: 6px;
                        }

                        .signature-name {
                            font-size: 14px;
                            font-style: italic;
                            color: #1f5ba7;
                            font-weight: 400;
                            margin-bottom: 4px;
                        }

                        .signature-meta {
                            font-size: 9.5px;
                            color: #5b6a80;
                            line-height: 1.35;
                        }

                        .footer {
                            position: absolute;
                            left: 0;
                            right: 0;
                            bottom: 0;
                            background: #1f5ba7;
                            color: #ffffff;
                            font-size: 7.8px;
                            padding: 5px 10px;
                            text-align: center;
                            line-height: 1.2;
                            box-sizing: border-box;
                        }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="topbar">
                            <table class="topbar-table">
                                <tr>
                                    <td class="brand-cell">
                                        <div class="brand-title">+ MeetCare</div>
                                        <div class="brand-subtitle">Plateforme de sante numerique</div>
                                    </td>
                                    <td class="badge-cell">
                                        <div class="badge">Ordonnance medicale</div>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <div class="meta-row">
                            <table class="meta-table">
                                <tr>
                                    <td><strong>Ref. :</strong> ORD-__ISSUE_STAMP__</td>
                                    <td><strong>Date d'emission :</strong> __ISSUE_DATE__</td>
                                    <td><strong>Validite :</strong> __VALIDITY__</td>
                                </tr>
                            </table>
                        </div>

                        <div class="body">
                            <table class="two-cols">
                                <tr>
                                    <td>
                                        <div class="card">
                                            <div class="card-head">Prescripteur</div>
                                            <div class="card-body">
                                                <div class="person-name">Dr. __DOCTOR_NAME__</div>
                                                <div class="person-line">__DOCTOR_SPECIALITY__</div>
                                                <div class="person-line">Cabinet MeetCare</div>
                                                <div class="person-line">Plateforme MeetCare</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="card">
                                            <div class="card-head">Patient</div>
                                            <div class="card-body">
                                                <div class="person-name">__PATIENT_NAME__</div>
                                                <div class="person-line">Document partage au patient</div>
                                                <div class="shared-note">Document partage avec le patient</div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </table>

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
                                            <th style="width: 14mm;">N</th>
                                            <th>Medicament / Prestation</th>
                                            <th>Posologie / Instructions</th>
                                            <th style="width: 22mm;">Duree</th>
                                            <th style="width: 32mm;">Remarques</th>
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
