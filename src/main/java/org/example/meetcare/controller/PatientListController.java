package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.example.meetcare.model.PatientListModel;
import org.example.meetcare.service.PatientListService;

public class PatientListController { // Corrigé : Suppression du 's' pour s'aligner sur le FXML

    @FXML private TextField txtRecherche;
    @FXML private Label lblNbPatients;
    @FXML private VBox vboxPatientsList;

    private final PatientListService patientService = new PatientListService();
    private List<PatientListModel> tousLesPatients = new ArrayList<>();
    private final int idDoctorConnecte = 1; 

    @FXML
    public void initialize() {
        rafraichirListe();

        txtRecherche.textProperty().addListener((obs, oldText, newText) -> {
            List<PatientListModel> listeFiltree = patientService.filtrerPatients(tousLesPatients, newText);
            afficherPatients(listeFiltree);
        });
    }

    private void rafraichirListe() {
        tousLesPatients = patientService.getPatientsParDocteur(idDoctorConnecte);
        afficherPatients(tousLesPatients);
    }

    private void afficherPatients(List<PatientListModel> liste) {
        vboxPatientsList.getChildren().clear();
        lblNbPatients.setText(liste.size() + " patient(s) trouvé(s)");

        for (PatientListModel p : liste) {
            vboxPatientsList.getChildren().add(creerCardPatient(p));
        }
    }

    private HBox creerCardPatient(PatientListModel patient) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(gaussian, #c8d8e8, 5, 0, 0, 1);");

        StackPane avatarPane = new StackPane();
        Circle circle = new Circle(28, javafx.scene.paint.Color.web("#F3E8FF"));
        String initiales = patient.getNom().replaceAll("(?i)(?:^|\\s)([a-z])[a-z]*", "$1").toUpperCase();
        Label lblInit = new Label(initiales);
        lblInit.setStyle("-fx-font-weight: bold; -fx-text-fill: #A855F7;");
        avatarPane.getChildren().addAll(circle, lblInit);

        VBox infosVBox = new VBox(4);
        HBox.setHgrow(infosVBox, Priority.ALWAYS);

        Label lblNom = new Label(patient.getNom());
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1A2C42;");

        HBox row1 = new HBox(15);
        Label lblEmail = new Label(patient.getEmail());
        lblEmail.setStyle("-fx-text-fill: #6B8CAD; -fx-font-size: 12px;");
        Label lblTel = new Label("•  " + patient.getTelephone());
        lblTel.setStyle("-fx-text-fill: #6B8CAD; -fx-font-size: 12px;");
        row1.getChildren().addAll(lblEmail, lblTel);

        HBox row2 = new HBox(15);
        Label lblRdv = new Label(patient.getNbRendezVous() + " rendez-vous");
        lblRdv.setStyle("-fx-text-fill: #6B8CAD; -fx-font-size: 11px;");
        
        String dateFormatee = patient.getDernierRDV() != null ? patient.getDernierRDV().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Aucun";
        Label lblDernierRdv = new Label("Dernier rendez-vous : " + dateFormatee);
        lblDernierRdv.setStyle("-fx-text-fill: #6B8CAD; -fx-font-size: 11px;");
        row2.getChildren().addAll(lblRdv, lblDernierRdv);

        infosVBox.getChildren().addAll(lblNom, row1, row2);

        Button btnDossier = new Button("Voir dossier");
        btnDossier.setStyle("-fx-background-color: #A855F7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        btnDossier.setOnAction(e -> afficherDescriptionPatient(patient));

        card.getChildren().addAll(avatarPane, infosVBox, btnDossier);
        return card;
    }

    private void afficherDescriptionPatient(PatientListModel patient) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
    dialog.setTitle("Dossier Médical - " + patient.getNom());
    dialog.setHeaderText("Fiche Patient : " + patient.getNom());
    
    // 1. Style the TextArea (Clean, borderless, modern font)
    TextArea txtDesc = new TextArea(patient.getDescription() != null ? patient.getDescription() : "Aucune note ou description enregistrée pour ce patient.");
    txtDesc.setEditable(false);
    txtDesc.setWrapText(true);
    txtDesc.setPrefHeight(180); // Slightly taller for better reading comfort
    txtDesc.setStyle(
        "-fx-font-size: 14px; " +
        "-fx-text-fill: #334155; " + // Slate color for soft, readable text
        "-fx-background-color: #F8FAFC; " + // Very light gray background
        "-fx-control-inner-background: #F8FAFC; " + // Internal area background
        "-fx-background-radius: 8; " +
        "-fx-border-color: #E2E8F0; " + // Soft subtle border
        "-fx-border-radius: 8; " +
        "-fx-padding: 10;"
    );
    
    // 2. Main Dialog Layout Styling
    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.setContent(txtDesc);
    dialogPane.setStyle(
        "-fx-background-color: #FFFFFF; " +
        "-fx-background-radius: 16; " +
        "-fx-border-radius: 16;"
    );
    
    // Remove the default ugly generic info icon
    dialog.setGraphic(null); 

    // 3. Style Header Text (Match your dashboard typography)
    Label headerLabel = (Label) dialogPane.lookup(".header-panel .label");
    if (headerLabel != null) {
        headerLabel.setStyle(
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1A2C42; " + // Your dark blue
            "-fx-padding: 10 0 10 0;"
        );
    }
    
    // Style the internal container of the header zone
    dialogPane.lookup(".header-panel").setStyle("-fx-background-color: #FFFFFF; -fx-padding: 15 20 5 20;");
    dialogPane.lookup(".content").setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10 20 15 20;");
    dialogPane.lookup(".button-bar").setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10 20 20 20;");

    // 4. Style the OK Action Button (Clean, rounded, purple)
    Button btnOk = (Button) dialogPane.lookupButton(ButtonType.OK);
    if (btnOk != null) {
        btnOk.setText("Fermer le dossier"); // Sound more medical/professional than just "OK"
        btnOk.setStyle(
            "-fx-background-color: #A855F7; " + // Purple theme
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 13px; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8 22; " +
            "-fx-cursor: hand;"
        );
        
        // Quick hover effect
        btnOk.setOnMouseEntered(e -> btnOk.setStyle("-fx-background-color: #9333EA; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 8 22; -fx-cursor: hand;"));
        btnOk.setOnMouseExited(e -> btnOk.setStyle("-fx-background-color: #A855F7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 8 22; -fx-cursor: hand;"));
    }
    
    dialog.showAndWait();
    }
}