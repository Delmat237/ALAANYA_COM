package com.alaanya.utils;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.alaanya.model.User;
import com.alaanya.socket.Client;

/*
 Class permettant d'authentifier un user
 */
public class AuthentificateUser {
      //Utilisation de CountDownLatch pour synchroniser l'attente de la réponse
    public static AuthenticationResult  auth(String militaryId, String password) throws SQLException, NoSuchAlgorithmException {

        //Hash le mot de passe
        String hashedPassword = User.hashPassword(password);
        System.out.println("auth : " + hashedPassword);
    
        Client.userId = militaryId;
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        CountDownLatch latch = new CountDownLatch(1); // Permet d'attendre la réponse
    
        // if (!Client.connectToServer()) {
        //     //si la connexion au serveur n'est pas possible
        //     return new AuthenticationResult(false, "Connexion au serveur central échouée.");
        // }
    
        //envoie une requete d'authentification
        Client.authUserRequest(militaryId, hashedPassword, notification -> {
            errorMessage.set(notification.getMessage());
            System.out.println("REP " + errorMessage.get());
            latch.countDown(); // Débloque le thread principal
        });
    
        try {
            latch.await(); // Attend la réponse du serveur avant de continuer
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AuthenticationResult(false, "Erreur d'attente de réponse du serveur.");
        }
    
        System.out.println("Final REP " + errorMessage.get());
    
        if (null == errorMessage.get()) {
            return new AuthenticationResult(false, "Identifiant militaire incorrect.");
        } else switch (errorMessage.get()) {
            case "TRUE" -> {
                System.out.println("on continue");
                return new AuthenticationResult(true, null);
            }
            case "MOT DE PASSE INCORRECT" -> {
                return new AuthenticationResult(false, "Mot de passe incorrect.");
            }
            default -> {
                return new AuthenticationResult(false, "Informations  incorrect.");
            }
        }
    }

}
