package org.example.meetcare.utils;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationUtil {

    public static void navigateTo(Node source, String fxmlPath, double width, double height, String title) {
        navigateToNode(source, fxmlPath, width, height, title);
    }

    public static void navigateTo(ActionEvent event, String fxmlPath, double width, double height, String title) {
        navigateToNode((Node) event.getSource(), fxmlPath, width, height, title);
    }

    public static void navigateTo(MouseEvent event, String fxmlPath, double width, double height, String title) {
        navigateToNode((Node) event.getSource(), fxmlPath, width, height, title);
    }

    private static void navigateToNode(Node source, String fxmlPath, double width, double height, String title) {
        try {
            Parent root = FXMLLoader.load(NavigationUtil.class.getResource(fxmlPath));
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(NavigationUtil.class.getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'ouvrir la page demandée.", ex);
        }
    }
}
