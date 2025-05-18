package controller;

import java.io.IOException;
import java.net.URL;

import socket.Client;
import model.Notification;
import utils.AuthenticationResult;
import utils.AuthentificateUser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@SuppressWarnings({"CallToPrintStackTrace","FieldMayBeFinal","UseSpecificCatch","exports"})

public class LoginController {

    @FXML private TextField militaryIdField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        //recuperation des informations sur l'interface

        String militaryId = militaryIdField.getText();
        String password = passwordField.getText();

        //Mise a jour du user courant
        Client.userId = militaryId;

        //verification des champs , si vides
        if (militaryId == null || militaryId.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        //Authentification du user
        try {
            //Recuperation du resultat d'authentification
            AuthenticationResult authResult = AuthentificateUser.auth(militaryId, password);

            System.out.println(authResult.isAuthenticated()); //aFFICHE LA REPONSE D'AUTHENTIFICATION

            if (authResult.isAuthenticated()) {
               
                //Nous devons pouvoir acceder a la plateforme meme sans etre connecté , mais enregistré oui
                
                boolean isConnected = Client.connectToServer(); //verifie la connexion au serveur centrale
                
                if (isConnected) {
                    System.out.println("openning view");

                    //On charge la page
                    ViewUtils.loadMainView(militaryId, errorLabel,1);
                } else {
                    ViewUtils.loadMainView(militaryId, errorLabel,0);
                    errorLabel.setText("Connexion au serveur Socket échouée.");
                }
            } else {

                errorLabel.setText(authResult.getErrorMessage());
            }
        } catch ( Exception e) {
            errorLabel.setText("Erreur de connexion à la base de données.");
             System.err.println(e.getMessage() + " : " + e.getCause());
        }
    }

    


    @FXML
    private void loadRegisterView() {
        try {
            URL url = getClass().getResource("/com/alaanya/view/RegisterView.fxml");
            if (url == null) {
                System.out.println("Fichier RegisterView.fxml non trouvé !");
            } else {
                System.out.println("Fichier trouvé : " + url);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alaanya/view/RegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) militaryIdField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            System.err.println(e.getMessage() + " : " + e.getCause());
            e.printStackTrace();
        }
    }

    public void onNotificationReceived(Notification notification) {
        Platform.runLater(() -> {
            errorLabel.setText("Notification reçue: " + notification.getMessage());
            if ("alerte".equals(notification.getType())) {
                errorLabel.setStyle("-fx-background-color: red; -fx-text-fill: white;");
            }
        });
    }
}
