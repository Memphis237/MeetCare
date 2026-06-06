import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class FxmlSmokeTest {
    public static void main(String[] args) {
        Platform.startup(() -> {
            test("/view/login.fxml");
            test("/view/homePatient.fxml");
            test("/view/invoicePatient.fxml");
            test("/view/invoiceMedecin.fxml");
            Platform.exit();
        });
    }

    private static void test(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(FxmlSmokeTest.class.getResource(path));
            Parent root = loader.load();
            System.out.println("OK: " + path + " -> " + root.getClass().getSimpleName());
        } catch (Exception ex) {
            System.err.println("KO: " + path);
            ex.printStackTrace();
        }
    }
}
