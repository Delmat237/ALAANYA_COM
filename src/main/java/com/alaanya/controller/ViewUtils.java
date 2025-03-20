package com.alaanya.controller;

import com.alaanya.model.User;
import com.alaanya.socket.Client;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal","exports"})

public class ViewUtils {

    public static void loadMainView(String militaryId, Label errorLabel) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewUtils.class.getResource("/com/alaanya/view/MainView.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();

            // Récupérer l'utilisateur depuis la base de données
            System.out.println("Recuperation des données depuis la bd centrale");
            User user = getUserByMilitaryId(militaryId);
            if (user != null) {
                System.out.println("Données recu : acces à l'application");
                mainController.setUser(user);
            } else {
                errorLabel.setText("Utilisateur introuvable dans la base de données.");
                return;
            }

            Scene scene = new Scene(root);
            Stage stage = (Stage) errorLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException | SQLException e) {
            errorLabel.setText("Erreur lors du chargement de la vue principale.");
            e.printStackTrace();
        }
    }
    // Méthode à sortir du LoginController et à mettre ici
    private static User getUserByMilitaryId(String militaryId) throws SQLException {
        return Client.getUserRequest(militaryId);

    }
}
