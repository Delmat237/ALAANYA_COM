module server.alaanyacentralserver {
    requires java.sql;
    requires com.google.gson;

    opens server.alaanyacentralserver to com.google.gson;
    opens server.model to com.google.gson;
    exports server.model;
}