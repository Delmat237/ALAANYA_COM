package com.alaanya;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@SuppressWarnings({"CallToPrintStackTrace","FieldMayBeFinal","exports"})

public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());
    private static Stage primaryStage;
    private StackPane rootLayout;

    @Override
    public void start(Stage primaryStage) {
        MainApp.primaryStage = primaryStage;
        configureStage();
        initRootLayout();
    }

    private void configureStage() {
        primaryStage.setTitle("ALAANYA-COM - Application de Communication Militaire");
        //primaryStage.getIcons().add(new Image("/com/alaanya/resources/com/alaanya/view/images/alaanya-com.png"));
       // primaryStage.setResizable(false);
    }

    public void initRootLayout() {
        try {
            // Load root layout from FXML file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/com/alaanya/view/LoginView.fxml"));
            rootLayout = (StackPane) loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
            primaryStage.setResizable(true);
            primaryStage.setMinHeight(800);
            primaryStage.setMinWidth(800);


        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur de chargement FXML", e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}

