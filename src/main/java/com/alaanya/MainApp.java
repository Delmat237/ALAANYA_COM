package com.alaanya;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal","exports"})

public class MainApp extends Application {

    private static Stage primaryStage;
    private GridPane rootLayout;

    @Override
    public void start(Stage primaryStage) {
        MainApp.primaryStage = primaryStage;
        MainApp.primaryStage.setTitle("ALAANYA-COM - Application de Communication Militaire");

        initRootLayout();
    }

    public void initRootLayout() {
        try {
            // Load root layout from FXML file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/com/alaanya/view/LoginView.fxml"));
            rootLayout = (GridPane) loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            //add style sheet
            //scene.getStylesheets().add(MainApp.class.getResource("/com/alaanya/view/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}

