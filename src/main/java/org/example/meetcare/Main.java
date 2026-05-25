package org.example.meetcare;

import java.io.IOException;

import org.example.meetcare.utils.DatabaseConnection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entree JavaFX.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/homeMedecin.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 850, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("Gestion des disponibilités");
            stage.setScene(scene);
            stage.show();
        }catch(IOException e) {
            e.printStackTrace();
            
        }
    }
    public static void main(String[] args) {
        launch(args); 
    } 

    @Override
    public void stop() {
        DatabaseConnection.closeConnection();
    }
}
