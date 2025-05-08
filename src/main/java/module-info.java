module com.alaanya.alaanya {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.swing;
    requires java.sql;
    requires java.desktop;
    requires com.google.gson;
    requires org.bytedeco.javacv;

    // Application
    opens com.alaanya to javafx.fxml;
    exports com.alaanya;

    // Contrôleurs
    opens controller to javafx.fxml;
    exports controller;

    // Modèles
    opens model to javafx.base, com.google.gson;
    exports model;

    // Modules audio / vidéo
    opens audio to javafx.fxml;
    exports audio;

    opens video to javafx.fxml;
    exports video;

    opens file to javafx.fxml, com.google.gson;
    exports file;

    opens message to javafx.fxml;
    exports message;
}
