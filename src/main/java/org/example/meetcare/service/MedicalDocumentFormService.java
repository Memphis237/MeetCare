package org.example.meetcare.service;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.example.meetcare.model.MedicalDocumentData;

import java.util.Locale;
import java.util.Optional;

public final class MedicalDocumentFormService {
    private static final String TYPE_ORDONNANCE = "Ordonnance";
    private static final String TYPE_COMPTE_RENDU = "Compte rendu";
    private static final String TYPE_ANALYSE = "Analyse";
    private static final String TYPE_RADIO = "Radio";
    private static final String TYPE_SCAN = "Scan";

    private MedicalDocumentFormService() {
    }

    public static Optional<MedicalDocumentData> showDocumentDialog(String patientName) {
        Dialog<MedicalDocumentData> dialog = new Dialog<>();
        dialog.setTitle("Nouveau document");
        dialog.setHeaderText("Creer un document pour " + patientName);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        ComboBox<String> typeComboBox = new ComboBox<>(FXCollections.observableArrayList(
                TYPE_ORDONNANCE, TYPE_COMPTE_RENDU, TYPE_ANALYSE, TYPE_RADIO, TYPE_SCAN
        ));
        typeComboBox.setValue(TYPE_ORDONNANCE);
        typeComboBox.setPrefWidth(260);

        TextField titleField = new TextField();
        titleField.setPromptText("Titre du document");

        TextField fieldOne = new TextField();
        TextField fieldTwo = new TextField();
        TextArea detailsArea = new TextArea();
        detailsArea.setWrapText(true);
        detailsArea.setPrefRowCount(6);
        GridPane.setHgrow(detailsArea, Priority.ALWAYS);

        Label fieldOneLabel = new Label();
        Label fieldTwoLabel = new Label();
        Label detailsLabel = new Label();

        grid.add(new Label("Type"), 0, 0);
        grid.add(typeComboBox, 1, 0);
        grid.add(new Label("Titre"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(fieldOneLabel, 0, 2);
        grid.add(fieldOne, 1, 2);
        grid.add(fieldTwoLabel, 0, 3);
        grid.add(fieldTwo, 1, 3);
        grid.add(detailsLabel, 0, 4);
        grid.add(detailsArea, 1, 4);

        Runnable refreshLabels = () -> {
            String type = typeComboBox.getValue();
            if (TYPE_ORDONNANCE.equals(type)) {
                titleField.setText(titleField.getText().isBlank() ? "Ordonnance" : titleField.getText());
                fieldOneLabel.setText("Traitement");
                fieldOne.setPromptText("Ex: Doliprane 1000mg");
                fieldTwoLabel.setText("Posologie");
                fieldTwo.setPromptText("Ex: 1 comprime matin et soir");
                detailsLabel.setText("Instructions");
                detailsArea.setPromptText("Duree du traitement, conseils complementaires...");
                return;
            }

            if (TYPE_COMPTE_RENDU.equals(type)) {
                titleField.setText(titleField.getText().isBlank() ? "Compte rendu de consultation" : titleField.getText());
                fieldOneLabel.setText("Motif");
                fieldOne.setPromptText("Ex: Consultation de suivi");
                fieldTwoLabel.setText("Conclusion");
                fieldTwo.setPromptText("Ex: Evolution favorable");
                detailsLabel.setText("Observations");
                detailsArea.setPromptText("Resume de la consultation, recommandations...");
                return;
            }

            if (TYPE_ANALYSE.equals(type)) {
                titleField.setText(titleField.getText().isBlank() ? "Resultats d'analyse" : titleField.getText());
                fieldOneLabel.setText("Analyse");
                fieldOne.setPromptText("Ex: Bilan sanguin");
                fieldTwoLabel.setText("Synthese");
                fieldTwo.setPromptText("Ex: Valeurs stables");
                detailsLabel.setText("Interpretation");
                detailsArea.setPromptText("Commentaires medicaux, suite a donner...");
                return;
            }

            if (TYPE_RADIO.equals(type)) {
                titleField.setText(titleField.getText().isBlank() ? "Radiographie" : titleField.getText());
                fieldOneLabel.setText("Zone examinee");
                fieldOne.setPromptText("Ex: Thorax");
                fieldTwoLabel.setText("Resultat");
                fieldTwo.setPromptText("Ex: Pas d'anomalie visible");
                detailsLabel.setText("Commentaires");
                detailsArea.setPromptText("Observations ou recommandations...");
                return;
            }

            titleField.setText(titleField.getText().isBlank() ? "Scanner" : titleField.getText());
            fieldOneLabel.setText("Zone examinee");
            fieldOne.setPromptText("Ex: Abdomen");
            fieldTwoLabel.setText("Resultat");
            fieldTwo.setPromptText("Ex: Exploration a completer");
            detailsLabel.setText("Commentaires");
            detailsArea.setPromptText("Observations ou recommandations...");
        };

        refreshLabels.run();
        typeComboBox.setOnAction(event -> refreshLabels.run());

        dialogPane.setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != javafx.scene.control.ButtonType.OK) {
                return null;
            }

            String type = typeComboBox.getValue();
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            String fieldOneValue = fieldOne.getText() == null ? "" : fieldOne.getText().trim();
            String fieldTwoValue = fieldTwo.getText() == null ? "" : fieldTwo.getText().trim();
            String details = detailsArea.getText() == null ? "" : detailsArea.getText().trim();

            if (title.isBlank() || fieldOneValue.isBlank() || fieldTwoValue.isBlank()) {
                return null;
            }

            String description = buildDescription(type, fieldOneLabel.getText(), fieldOneValue, fieldTwoLabel.getText(), fieldTwoValue, detailsLabel.getText(), details);
            String filePath = "records/" + slugify(type) + "-" + System.currentTimeMillis() + ".txt";
            return new MedicalDocumentData(title, type, description, filePath);
        });

        return dialog.showAndWait();
    }

    private static String buildDescription(String type, String fieldOneLabel, String fieldOneValue,
                                           String fieldTwoLabel, String fieldTwoValue,
                                           String detailsLabel, String details) {
        return """
                Type : %s
                %s : %s
                %s : %s
                %s : %s
                """.formatted(
                type,
                fieldOneLabel, fieldOneValue,
                fieldTwoLabel, fieldTwoValue,
                detailsLabel, details.isBlank() ? "Aucune precision complementaire." : details
        );
    }

    private static String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
