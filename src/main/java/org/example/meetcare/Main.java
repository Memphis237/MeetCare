package org.example.meetcare;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Point d'entree JavaFX.
 */
public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = Objects.requireNonNull(
                Main.class.getResource("/view/homePatient.fxml"),
                "Missing FXML resource: /view/homePatient.fxml"
        );
        URL stylesheet = Objects.requireNonNull(
                Main.class.getResource("/css/style.css"),
                "Missing stylesheet resource: /css/style.css"
        );

        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        Scene scene = new Scene(fxmlLoader.load(), 620, 520);
        scene.getStylesheets().add(stylesheet.toExternalForm());
        stage.setTitle("Bibliothèque - Gestion des livres");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
