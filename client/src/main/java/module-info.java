module com.example.client {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.ex to javafx.fxml;
    exports com.example.ex;
}