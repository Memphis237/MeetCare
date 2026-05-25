package org.example.meetcare.controller;

import java.io.IOException;
import javafx.scene.Node;

import org.example.meetcare.utils.LogoutUtil;
import javafx.scene.input.MouseEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;


public class SidebarMedecinController {

    @FXML
    private VBox sidebarContainer; // Changé en VBox pour correspondre au FXML !

    @FXML
    private Label lblInitialesDoc;
    @FXML
    private Label lblNomDoc;
    @FXML
    private Label lblSpecialiteDoc;

    /**
     * Méthode générique pour charger une vue au centre du BorderPane principal
     */
    private void switchView(String fxmlPath) {
        try {
            // 1. On récupère le BorderPane qui contient cette sidebar
            BorderPane mainBorderPane = (BorderPane) sidebarContainer.getParent();
            
            // 2. On charge le nouveau fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newView = loader.load();
            
            // 3. On l'installe au centre de l'application
            mainBorderPane.setCenter(newView);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur de chargement de la vue : " + fxmlPath);
        } catch (ClassCastException e) {
            System.err.println("Erreur : Le parent de la sidebar n'est pas un BorderPane.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS DE NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void handleNavHome() {
        switchView("/view/dashboardMedecin.fxml"); // Ta vue d'accueil de base
    }

    @FXML
    private void handleNavAgenda() {
        switchView("/view/Agenda.fxml");
    }

    @FXML
    private void handleNavPatients() {
        switchView("/view/patientList.fxml");
    }

    @FXML
    private void handleNavDisponibilites() {
        switchView("/view/disponibilites.fxml");
    }
    @FXML
    private void handleNavDocuments() {
        switchView("/view/Documents.fxml");
    }

    @FXML
    private void handleNavFacturation() {
        switchView("/view/Facturation.fxml");
    }

    @FXML
    private void handleNavMessages() {
        switchView("/view/Messages.fxml");
    }

    @FXML
    private void handleNavStatistiques() {
        switchView("/view/Statistiques.fxml");
    }

    @FXML
    private void handleNavProfilPublic() {
        switchView("/view/ProfilPublic.fxml");
    }

    @FXML
    private void handleNavParametres() {
        switchView("/view/Parametres.fxml");
    }
    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}