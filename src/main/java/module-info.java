module org.example.meetcare {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;

    opens org.example.meetcare to javafx.fxml;
    opens org.example.meetcare.controller to javafx.fxml;

    exports org.example.meetcare;
}
