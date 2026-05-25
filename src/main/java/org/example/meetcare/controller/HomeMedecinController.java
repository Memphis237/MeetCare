package org.example.meetcare.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import java.io.IOException;

import org.example.meetcare.utils.LogoutUtil;
import org.example.meetcare.utils.NavigationUtil;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Role;
import org.example.meetcare.model.Users;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.utils.AvatarUtil;

public class HomeMedecinController {

    // On injecte le ScrollPane central
    @FXML
    private ScrollPane mainScrollPane;

    /**
     * Méthode utilitaire pour changer le contenu central
     * @param fxmlPath Le chemin vers le fichier FXML à charger (ex: "/fxml/Disponibilites.fxml")
     */
    private void loadCentralView(String fxmlPath) {
        try {
            // 1. Charger le nouveau fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            
            // 2. Remplacer le contenu du ScrollPane central
            mainScrollPane.setContent(newContent);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de la vue : " + fxmlPath);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  GESTION DES CLICS DE LA SIDEBAR
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void handleDisponibilites(MouseEvent event) {
        // Quand on clique sur "Disponibilités", on charge le FXML correspondant au centre
        loadCentralView("/fxml/disponibilites.fxml"); 
    }

    
    
    @FXML
    private void handlePatients(MouseEvent event) {
        loadCentralView("/fxml/patientList.fxml");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        LogoutUtil.logout((Node) event.getSource());
    }
}