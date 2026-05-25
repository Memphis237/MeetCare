package org.example.meetcare.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.io.IOException;

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
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'ouvrir la page demandée.", ex);
        }
    }
}
