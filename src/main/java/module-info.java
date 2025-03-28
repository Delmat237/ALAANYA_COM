module com.alaanya.alaanya {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires javafx.graphics;


    opens com.alaanya to javafx.graphics;
    exports com.alaanya;

    opens com.alaanya.controller to javafx.fxml;
    exports com.alaanya.controller;

    opens com.alaanya.model to javafx.base;
    exports com.alaanya.model;


}
