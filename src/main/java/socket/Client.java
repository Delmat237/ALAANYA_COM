package socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import database.Database;
import model.Message;
import model.Notification;
import model.User;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    private static Socket socketCentral;
    private static BufferedWriter outCentral;
    private static BufferedReader inCentral;

    private static boolean isConnectedToServer = false;
    public static String userId;

    // Initialisation de l'objet Gson avec format de date
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

    // Adresse IP et port du serveur central
    private static final String CENTRAL_SERVER_IP = "10.2.64.14";
    private static final int CENTRAL_SERVER_PORT = 7000;

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    // Interface pour gérer les notifications reçues
    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
    }

    // Connexion au serveur central
    public static boolean connectToServer() {
        if (isConnectedToServer && socketCentral != null && socketCentral.isConnected()) {
            return true;
        }
        try {
            socketCentral = new Socket(CENTRAL_SERVER_IP, CENTRAL_SERVER_PORT);
            outCentral = new BufferedWriter(new OutputStreamWriter(socketCentral.getOutputStream()));
            inCentral = new BufferedReader(new InputStreamReader(socketCentral.getInputStream()));
            isConnectedToServer = true;
            logger.info("[CLIENT] Connected to central server");
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Connection error: " + e.getMessage());
            isConnectedToServer = false;
            return false;
        }
    }

    // Envoie une requête pour obtenir l'adresse d'un utilisateur
    public static void requestAddress(String userId, NotificationListener listener) {
        if (!connectToServer()) {
            logger.log(Level.SEVERE, "[CLIENT] Cannot request address, not connected.");
            return;
        }
        try {
            Message request = new Message("SERVER", "REQUEST_ADDRESS", userId, "CENTRAL_SERVER");
            outCentral.write(gson.toJson(request) + "\n");
            outCentral.flush();
            logger.info("[JSON REQUEST]" + gson.toJson(request) );
            String jsonResponse = inCentral.readLine();
            Notification response = gson.fromJson(jsonResponse, Notification.class);

            if (response != null) {
                listener.onNotificationReceived(response);
                logger.info("[CLIENT] Address response received.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error in address request: " + e.getMessage());
        }
    }

    // Authentifie un utilisateur via le serveur central, ou localement en cas d'échec
    public static void authUserRequest(String userId, String password, NotificationListener listener) throws SQLException {
        if (!connectToServer()) {
            String msg = Database.authUser(userId, password);
            String[] parts = msg.split("&&");
            listener.onNotificationReceived(new Notification(parts[0], parts[1], 5, "LOCALHOST"));
            return;
        }
        try {
            String payload = userId + "&&" + password;
            Message request = new Message("SERVER", "AUTHENTICATE_USER", payload, "CENTRAL_SERVER");

            logger.info("[JSON REQUEST]" + gson.toJson(request) );
            outCentral.write(gson.toJson(request) + "\n");
            outCentral.flush();

            String jsonResponse = inCentral.readLine();
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null) {
                listener.onNotificationReceived(notification);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Auth request error: " + e.getMessage());
        }
    }

    // Envoie une requête pour ajouter un nouvel utilisateur
    public static void addUserRequest(String military_id, String password_hash, String grade, String division, String username) throws SQLException, NoSuchAlgorithmException {
        if (!connectToServer()) {
            logger.log(Level.SEVERE, "[CLIENT] Not connected to central server.");
            return;
        }
        try {
            String payload = String.join("&&", military_id, password_hash, grade, division, username);
            Message request = new Message("SERVER", "SAVE_USER", payload, "CENTRAL_SERVER");

            logger.info("[JSON REQUEST]" + gson.toJson(request) );

            outCentral.write(gson.toJson(request) + "\n");
            outCentral.flush();

            String jsonResponse = inCentral.readLine();
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null && "TRUE".equals(notification.getMessage())) {
                User user = new User(military_id, grade, division, username);
                user.setPasswordHash(password_hash);
                Database.addUser(user);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error saving user: " + e.getMessage());
        }
    }

    // Récupère les informations d'un utilisateur depuis le serveur central ou la base locale
    public static User getUserRequest(String military_id) throws SQLException {
        if (!connectToServer()) {
            return Database.getUser(military_id);
        }
        try {
            Message request = new Message("SERVER", "GET_USER", military_id, "CENTRAL_SERVER");

            logger.info("[JSON REQUEST]" + gson.toJson(request) );

            outCentral.write(gson.toJson(request) + "\n");
            outCentral.flush();

            String jsonResponse = inCentral.readLine();
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null && notification.getMessage() != null) {
                String[] parts = notification.getMessage().split("&&");
                User user = new User(parts[0], parts[1], parts[2], parts[3]);
                user.setPasswordHash(parts[4]);
                return user;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error retrieving user: " + e.getMessage());
        }
        return null;
    }

    // Déconnecte le client du serveur central
    public static void disconnectFromServer() {
        try {
            if (socketCentral != null && !socketCentral.isClosed()) {
                socketCentral.close();
            }
            isConnectedToServer = false;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error while disconnecting: " + e.getMessage());
        }
    }
}
