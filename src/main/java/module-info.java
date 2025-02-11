module alaanya {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;
    //requires opencv;


   /* opens ald.alaanya_com to javafx.fxml;
    exports ald.alaanya_com;*/
    exports alaanya;
    opens alaanya to javafx.fxml;
    //exports video to javafx.graphics;
}