module com.alaanya.alaanya {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    // Ajoutez ces lignes
    opens com.alaanya to javafx.graphics;
    exports com.alaanya;

    opens com.alaanya.controller to javafx.fxml;
    exports com.alaanya.controller;

    opens com.alaanya.model to javafx.base;
    exports com.alaanya.model;

    opens com.alaanya.view to javafx.fxml;
}
