package org.example.meetcare;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Point d'entree JavaFX.
 */
public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
       FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/register.fxml"));
       Scene scene = new Scene(fxmlLoader.load(), 500, 650);
       scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
       stage.setTitle("MeetCare - Inscription");
       stage.setResizable(true);        // ✅ permet de redimensionner
       stage.setMinWidth(460);          // ✅ taille minimum
       stage.setMinHeight(600);         // ✅ taille minimum
       stage.setScene(scene);
       stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
