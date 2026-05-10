module org.example.meetcare {
    requires javafx.controls;
    requires javafx.fxml;
    requires jbcrypt;
    requires java.sql;

    opens org.example.meetcare to javafx.fxml;
    opens org.example.meetcare.controller to javafx.fxml;
    opens org.example.meetcare.model to javafx.base;

    exports org.example.meetcare;
}
