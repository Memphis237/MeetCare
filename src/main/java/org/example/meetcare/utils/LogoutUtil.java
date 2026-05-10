package org.example.meetcare.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.example.meetcare.dao.UsersDao;
import org.example.meetcare.model.Users;

import java.io.IOException;
import java.util.Optional;

public final class LogoutUtil {
    private static final double LOGIN_WIDTH = 620;
    private static final double LOGIN_HEIGHT = 520;

    private LogoutUtil() {
    }

    public static void logout(Node source) {
        logout(source, new UsersDao());
    }

    public static void logout(Node source, UsersDao usersDao) {
        if (!confirmLogout(source)) {
            return;
        }

        Users currentUser = AppSession.getCurrentUser();
        try {
            if (currentUser != null && usersDao != null) {
                usersDao.updateOnlineStatus(currentUser.getIdUsers(), false);
            }
        } catch (RuntimeException ignored) {
            // We keep the local logout even if the database update fails.
        }

        AppSession.clear();

        try {
            Parent root = FXMLLoader.load(LogoutUtil.class.getResource("/view/login.fxml"));
            Scene scene = new Scene(root, LOGIN_WIDTH, LOGIN_HEIGHT);
            scene.getStylesheets().add(LogoutUtil.class.getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("MeetCare - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible to return to the login page.", ex);
        }
    }

    private static boolean confirmLogout(Node source) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de déconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous déconnecter ?");
        alert.setContentText("Vous devrez vous reconnecter pour accéder à votre espace.");

        if (source != null && source.getScene() != null && source.getScene().getWindow() != null) {
            alert.initOwner(source.getScene().getWindow());
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
