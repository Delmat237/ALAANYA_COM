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

@SuppressWarnings({"CallToPrintStackTrace", "FieldMayBeFinal"})
public class Client {

    private static Socket socketCentral;
    private static BufferedWriter outCentral;
    private static BufferedReader inCentral;

    private static boolean isConnectedToServer = false;
    public static String userId;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

    private static final String CENTRAL_SERVER_IP = "10.2.64.14";
    private static final int CENTRAL_SERVER_PORT = 7000;

    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
    }

    /**
     * Connexion au serveur central
     */
    public static boolean connectToServer() {
        if (isConnectedToServer && socketCentral != null && socketCentral.isConnected()) {
            return true; // Déjà connecté
        }

        try {
            socketCentral = new Socket(CENTRAL_SERVER_IP, CENTRAL_SERVER_PORT);
            outCentral = new BufferedWriter(new OutputStreamWriter(socketCentral.getOutputStream()));
            inCentral = new BufferedReader(new InputStreamReader(socketCentral.getInputStream()));

            isConnectedToServer = true;
            System.out.println("[CLIENT] Connected to central server");

            return true;
        } catch (IOException e) {
            System.err.println("[CLIENT] Error connecting to central server: " + e.getMessage());
            isConnectedToServer = false;
            return false;
        }
    }

    public static void requestAddress(String userId, NotificationListener listener) {
        if (!connectToServer()) {
            System.err.println("[CLIENT] Cannot request address, not connected.");
            return;
        }

        try {
            Message requestMessage = new Message("SERVER", "REQUEST_ADDRESS", userId, "CENTRAL_SERVER");
            String jsonRequest = gson.toJson(requestMessage);

            outCentral.write(jsonRequest + "\n");
            outCentral.flush();

            System.out.println("[CLIENT] Requesting address for: " + userId);

            String jsonResponse = inCentral.readLine();
            Notification response = gson.fromJson(jsonResponse, Notification.class);

            if (response != null) {
                listener.onNotificationReceived(response);
                System.out.println("[CLIENT] Received address: " + response.getMessage());
            } else {
                System.err.println("[CLIENT] Invalid response received.");
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Error during address request: " + e.getMessage());
        }
    }

    public static void authUserRequest(String userId, String password, NotificationListener listener) throws SQLException {
        if (!connectToServer()) {
            String msg = Database.authUser(userId, password);
            listener.onNotificationReceived(new Notification(msg.split("&&")[0], msg.split("&&")[1], 5, "LOCALHOST"));
            System.out.println("[CLIENT] Local auth: " + msg);
            return;
        }

        try {
            Message requestMessage = new Message("SERVER", "AUTHENTICATE_USER", userId + "&&" + password, "CENTRAL_SERVER");
            String jsonRequest = gson.toJson(requestMessage);

            outCentral.write(jsonRequest + "\n");
            outCentral.flush();

            String jsonResponse = inCentral.readLine();
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null) {
                listener.onNotificationReceived(notification);
                System.out.println("[CLIENT] Auth response: " + notification.getMessage());
            } else {
                System.err.println("[CLIENT] Auth failed: null response");
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Auth error: " + e.getMessage());
        }
    }

    public static void addUserRequest(String military_id, String password_hash, String grade, String division, String username) throws SQLException, NoSuchAlgorithmException {
        if (!connectToServer()) {
            System.err.println("[CLIENT] Not connected to central server.");
            return;
        }

        try {
            String payload = military_id + "&&" + password_hash + "&&" + grade + "&&" + division + "&&" + username;
            Message requestMessage = new Message("SERVER", "SAVE_USER", payload, "CENTRAL_SERVER");

            String jsonRequest = gson.toJson(requestMessage);
            outCentral.write(jsonRequest + "\n");
            outCentral.flush();

            System.out.println("[CLIENT] Saving user: " + military_id);

            String jsonResponse = inCentral.readLine();
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null && "TRUE".equals(notification.getMessage())) {
                User user = new User(military_id, grade, division, username);
                user.setPasswordHash(password_hash);
                Database.addUser(user);
                System.out.println("[CLIENT] User saved locally.");
            } else {
                System.err.println("[CLIENT] User save failed or rejected.");
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Error saving user: " + e.getMessage());
        }
    }

    public static User getUserRequest(String military_id) throws SQLException {
        if (!connectToServer()) {
            return Database.getUser(military_id);
        }

        try {
            Message requestMessage = new Message("SERVER", "GET_USER", military_id, "CENTRAL_SERVER");
            String jsonRequest = gson.toJson(requestMessage);

            outCentral.write(jsonRequest + "\n");
            outCentral.flush();

            System.out.println("[CLIENT] Getting user info for: " + military_id);

            String jsonResponse = inCentral.readLine();
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null && notification.getMessage() != null) {
                String[] parts = notification.getMessage().split("&&");
                User user = new User(parts[0], parts[1], parts[2], parts[3]);
                user.setPasswordHash(parts[4]);
                return user;
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("[CLIENT] Error retrieving user: " + e.getMessage());
        }

        return null;
    }

    /**
     * Déconnexion du serveur central
     */
    public static void disconnectFromServer() {
        try {
            if (socketCentral != null && !socketCentral.isClosed()) {
                socketCentral.close();
            }
            isConnectedToServer = false;
            System.out.println("[CLIENT] Disconnected from server.");
        } catch (IOException e) {
            System.err.println("[CLIENT] Error while disconnecting: " + e.getMessage());
        }
    }
}
