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

    // Initialisation de Gson avec un format de date standard
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

    // Informations du serveur central
    private static final String CENTRAL_SERVER_IP = "192.168.207.11";

    private static final int CENTRAL_SERVER_PORT = 7000;

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    // Interface pour réception des notifications
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

    // Méthode utilitaire pour envoyer une requête JSON et recevoir une Notification
    private static Notification sendRequest(Message request) throws IOException {
        if (!connectToServer()) {
            logger.severe("[CLIENT] Cannot send request, not connected to server.");
            return null;
        }

        // Envoi de la requête
        String jsonRequest = gson.toJson(request);
        outCentral.write(jsonRequest + "\n");
        outCentral.flush();
        logger.info("[JSON REQUEST] " + jsonRequest);

        // Lecture de la réponse
        String jsonResponse = inCentral.readLine();
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            logger.warning("[CLIENT] No response from server.");
            return null;
        }

        logger.info("[JSON RESPONSE] " + jsonResponse);
        return gson.fromJson(jsonResponse, Notification.class);
    }

    // Requête d'adresse d'un utilisateur
    public static void requestAddress(String userId, NotificationListener listener) {
        try {
            Message request = new Message("SERVER", "REQUEST_ADDRESS", userId, "CENTRAL_SERVER");
            Notification response = sendRequest(request);
            if (response != null) {
                listener.onNotificationReceived(response);
                logger.info("[CLIENT] Address response received.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error in address request: " + e.getMessage());
        }
    }

    // Authentifie un utilisateur (via serveur ou base locale en fallback)
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
            Notification notification = sendRequest(request);

            if (notification != null) {
                listener.onNotificationReceived(notification);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Auth request error: " + e.getMessage());
        }
    }

    // Ajoute un utilisateur via le serveur, puis l'insère localement si succès
    public static void addUserRequest(String military_id, String password_hash, String grade, String division, String username)
            throws SQLException, NoSuchAlgorithmException {
        try {
            String payload = String.join("&&", military_id, password_hash, grade, division, username);
            Message request = new Message("SERVER", "SAVE_USER", payload, "CENTRAL_SERVER");
            Notification notification = sendRequest(request);

            if (notification != null && "TRUE".equals(notification.getMessage())) {
                User user = new User(military_id, grade, division, username);
                user.setPasswordHash(password_hash);
                Database.addUser(user);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error saving user: " + e.getMessage());
        }
    }

    // Récupère un utilisateur (serveur ou base locale)
    public static User getUserRequest(String military_id) throws SQLException {
        if (!connectToServer()) {
            return Database.getUser(military_id);
        }

        try {
            Message request = new Message("SERVER", "GET_USER", military_id, "CENTRAL_SERVER");
            Notification notification = sendRequest(request);

            if (notification != null && notification.getMessage() != null) {
                String[] parts = notification.getMessage().split("&&");
                if (parts.length >= 5) {
                    User user = new User(parts[0], parts[1], parts[2], parts[3]);
                    user.setPasswordHash(parts[4]);
                    return user;
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error retrieving user: " + e.getMessage());
        }

        return null;
    }

    // Déconnexion du serveur central
    public static void disconnectFromServer() {
        try {
            if (socketCentral != null && !socketCentral.isClosed()) {
                socketCentral.close();
            }
            isConnectedToServer = false;
            logger.info("[CLIENT] Disconnected from server.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CLIENT] Error while disconnecting: " + e.getMessage());
        }
    }
}
