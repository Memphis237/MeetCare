module org.example.meetcare {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jbcrypt;
    requires java.sql;
    requires java.net.http;
    requires openhtmltopdf.core;
    requires openhtmltopdf.pdfbox;

    opens org.example.meetcare to javafx.fxml;
    opens org.example.meetcare.controller to javafx.fxml;
    opens org.example.meetcare.model to javafx.base;

    exports org.example.meetcare;
}
