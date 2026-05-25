package org.example.meetcare.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.example.meetcare.model.Creneau;
import org.example.meetcare.service.DisponibiliteService;

import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.skin.DatePickerSkin;

public class DisponibilitesController {

    // Liens avec les 4 cartes statistiques du FXML
    @FXML private Label lblTotal, lblDispo, lblOccupe, lblTaux;
    // Et ajoute celle-ci juste en dessous :
    @FXML private Label labelCreneaux;
    // Formulaire d'ajout
    @FXML private TextField txtHeureDebut;
    @FXML private TextField txtHeureFin;
    
    // Éléments interactifs centraux
    @FXML private GridPane gridCreneaux; 
    @FXML private DatePicker datePicker;
    @FXML private VBox calendarPlaceHolder; // Conteneur pour le calendrier

    private final DisponibiliteService service = new DisponibiliteService();
    private LocalDate dateSelectionnee = LocalDate.now(); 

    @FXML
    public void initialize() {
        DatePicker localDatePicker = new DatePicker(LocalDate.now());
        DatePickerSkin datePickerSkin = new DatePickerSkin(localDatePicker);
        var justeLeCalendrier = datePickerSkin.getPopupContent();
        
        if (calendarPlaceHolder != null) {
            calendarPlaceHolder.getChildren().add(justeLeCalendrier);
        }
        
        // ÉCOUTEUR : Quand on clique sur un jour du calendrier, on met à jour la vue
        localDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                this.dateSelectionnee = newDate; // On mémorise la date cliquée
                rafraichirInterface();           // On recharge les créneaux et les KPI de ce jour
            }
        });
        
        rafraichirInterface();
    }

    @FXML
    private void handleAjouterCreneau() {
        String debut = txtHeureDebut.getText().trim();
        String fin = txtHeureFin.getText().trim();

        if (!debut.isEmpty() && !fin.isEmpty()) {
            String horaireFormate = debut + " - " + fin;
            
            try {
                service.ajouterCreneau(0, dateSelectionnee, LocalTime.parse(debut), LocalTime.parse(fin), true, 1);
                
                // Nettoyage des champs de saisie
                txtHeureDebut.clear();
                txtHeureFin.clear();
                
                // Recalcul global immédiat
                rafraichirInterface();
                System.out.println("Créneau ajouté : " + horaireFormate);
                
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Erreur : Le format de l'heure doit être HH:mm (ex: 09:30)");
            }
        }
    }

    private void rafraichirInterface() {
        // 1. On récupère d'ABORD la liste fraîche depuis la BDD pour la date sélectionnée
        List<Creneau> listeActive = service.getCreneauxParDate(dateSelectionnee);

        System.out.println("🔍 [DEBUG] Nombre de créneaux reçus pour le " + dateSelectionnee + " : " + listeActive.size());

        // 2. Mise à jour dynamique des compteurs (KPI) en injectant la liste active
        if (lblTotal != null) lblTotal.setText(String.valueOf(service.getTotal(listeActive)));
        if (lblDispo != null) lblDispo.setText(String.valueOf(service.getDisponibles(listeActive)));
        if (lblOccupe != null) lblOccupe.setText(String.valueOf(service.getOccupes(listeActive)));
        if (lblTaux != null) lblTaux.setText(service.getTaux(listeActive) + "%");
        if (labelCreneaux.getText().isEmpty()) {
        labelCreneaux.setText("Créneaux du " + dateSelectionnee);
    }
        
        // 3. Nettoyage et reconstruction du GridPane des cartes horaires
        if (gridCreneaux != null) {
            gridCreneaux.getChildren().clear();
            
            int colonnesMax = 2;
            for (int i = 0; i < listeActive.size(); i++) {
                Creneau c = listeActive.get(i);
                int ligne = i / colonnesMax;
                int col = i % colonnesMax;
                gridCreneaux.add(genererCardCreneau(c), col, ligne);
            }            
        }
    }

    private HBox genererCardCreneau(Creneau creneau) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER_RIGHT);
        container.setPadding(new Insets(15));
        
        VBox textVBox = new VBox(5);
        HBox.setHgrow(textVBox, Priority.ALWAYS);
        
        Label lblTime = new Label(creneau.getPlageHoraire());
        lblTime.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2A3547;");
        Label badgeStatus = new Label();
        
        if (creneau.isDisponible()) {
            container.getStyleClass().add("slot-available");
            badgeStatus.setText("Disponible");
            badgeStatus.getStyleClass().add("badge-available");
            
            Button btnDelete = new Button("✕");
            btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #7C8BA1; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> {
                service.supprimerCreneau(creneau);
                rafraichirInterface();
            });
            
            textVBox.getChildren().addAll(lblTime, badgeStatus);
            container.getChildren().addAll(textVBox, btnDelete);
        } else {
            container.getStyleClass().add("slot-reserved");
            badgeStatus.setText("Réservé");
            badgeStatus.getStyleClass().add("badge-reserved");
            
            textVBox.getChildren().addAll(lblTime, badgeStatus);
            container.getChildren().addAll(textVBox);
        }

        return container;
    }
}