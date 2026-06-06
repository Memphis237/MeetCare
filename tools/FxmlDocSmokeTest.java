import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class FxmlDocSmokeTest {
  public static void main(String[] args) throws Exception {
    CountDownLatch startup = new CountDownLatch(1);
    Platform.startup(startup::countDown);
    startup.await();
    test("/view/documentsPatient.fxml");
    test("/view/documentsMedecin.fxml");
    Platform.exit();
  }
  static void test(String path) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    Platform.runLater(() -> {
      try {
        Parent root = FXMLLoader.load(FxmlDocSmokeTest.class.getResource(path));
        System.out.println(path + " OK -> " + root.getClass().getSimpleName());
      } catch (Throwable t) {
        error.set(t);
      } finally {
        latch.countDown();
      }
    });
    latch.await();
    if (error.get() != null) throw new RuntimeException(path, error.get());
  }
}
