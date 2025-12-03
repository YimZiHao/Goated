module com.mycompany.smartjournaling {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;

    opens com.mycompany.smartjournaling to javafx.fxml;
    exports com.mycompany.smartjournaling;
}
