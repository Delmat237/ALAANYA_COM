module org.example.alaanya {
    //requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;


    opens org.example.alaanya to javafx.fxml;
    exports org.example.alaanya;
    exports alaanya;
    opens alaanya to javafx.fxml;
}
