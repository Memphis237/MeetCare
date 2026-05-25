package org.example.meetcare.utils;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationUtil {

    public static void navigateTo(Event event, String fxmlPath, double width, double height, String title) {
        try {
            Parent root = FXMLLoader.load(NavigationUtil.class.getResource(fxmlPath));
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(NavigationUtil.class.getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception ex) {
            System.err.println("Erreur lors de l'ouverture de la page " + fxmlPath + ":");
            ex.printStackTrace();
            throw new IllegalStateException("Impossible d'ouvrir la page demandee.", ex);
        }
    }
}
