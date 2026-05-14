package org.example.meetcare;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.meetcare.utils.DatabaseConnection;

import java.io.IOException;

/**
 * Point d'entree JavaFX.
 */
public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/homePatient.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 850, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("Bibliothèque - Gestion des livres");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        DatabaseConnection.closeConnection();
    }
}
