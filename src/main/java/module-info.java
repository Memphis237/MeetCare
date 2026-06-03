module org.example.meetcare {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jbcrypt;
    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires openhtmltopdf.core;
    requires openhtmltopdf.pdfbox;
    requires org.apache.pdfbox;

    opens org.example.meetcare to javafx.fxml;
    opens org.example.meetcare.controller to javafx.fxml;
    opens org.example.meetcare.model to javafx.base;

    exports org.example.meetcare;
}
