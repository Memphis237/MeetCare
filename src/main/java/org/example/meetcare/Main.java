package org.example.meetcare;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.meetcare.utils.DatabaseConnection;
import org.example.meetcare.utils.EnvLoader;
import org.example.meetcare.utils.AppSession;
import org.example.meetcare.dao.UsersDao;

import java.io.IOException;

/**
 * Point d'entree JavaFX.
 */
public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        EnvLoader.load();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
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
        if (AppSession.getCurrentUser() != null) {
            try {
                new UsersDao().updateOnlineStatus(AppSession.getCurrentUser().getIdUsers(), false);
            } catch (Exception ignored) {
                // We keep shutdown resilient even if presence syncing fails.
            }
        }
        DatabaseConnection.closeConnection();
    }
}
