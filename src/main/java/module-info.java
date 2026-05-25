module org.example.meetcare {
    requires javafx.controls;
    requires javafx.fxml;
    requires jbcrypt;
    requires java.sql;
    requires java.net.http;

    opens org.example.meetcare to javafx.fxml;
    opens org.example.meetcare.controller to javafx.fxml;
    opens org.example.meetcare.model to javafx.base;

    exports org.example.meetcare;
}
