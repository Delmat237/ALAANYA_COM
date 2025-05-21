package socket;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import database.Database;
import model.Message;
import model.Notification;
import model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@SuppressWarnings({"CallToPrintStackTrace","FieldMayBeFinal"})

public class Client {

    private static BufferedWriter outCentral;
    private static BufferedReader inCentral;
    private static Socket socketCentral;

    private static boolean isConnectedToServer = false;
    public static String userId;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

    private static final String CENTRAL_SERVER_IP = "10.2.64.89";
    private static final int CENTRAL_SERVER_PORT = 7000;

    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
    }

    private static NotificationListener notificationListener;


    //Connection au serveur centrale
    public static boolean connectToServer(){
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
    
    /*
     * params
     * userId : id of the user
     * NotificationListener : listener to notify when the address is received
     * ACTION :  
     * allow to take user address
     */
    public static void requestAddress(String userId, NotificationListener listener) {
        if (!isConnectedToServer) {
            System.err.println("[CLIENT] Not connected to central server.");
            return;
        }

        try {
            // Send request for user's address
            System.out.println("j'envoie la requete");
            Message requestMessage = new Message("SERVER", "REQUEST_ADDRESS", userId,"CENTRAL_SERVER");
            System.out.println("le message est constitué");

            // Assure-toi que la connexion est établie et que outCentral/inCentral sont initialisés

            connectToServer();
            // Sérialiser en JSON et envoyer
            String jsonRequest = gson.toJson(requestMessage);

            System.out.println("[CLIENT] Sending JSON: " + jsonRequest);
            outCentral.write(jsonRequest +"\n");
            outCentral.flush();

            System.out.println("[CLIENT] Address request sent for user: " + userId);

            // Bon :
            String jsonResponse = inCentral.readLine();
            Notification response = gson.fromJson(jsonResponse, Notification.class);

            if (response != null) {
                listener.onNotificationReceived(response);
                System.out.println("[CLIENT] Received address response: " + response.getMessage());
            } else {
                System.err.println("[CLIENT] Received null or invalid notification.");
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting Address: " + e.getMessage());
            e.printStackTrace();

        }
    }
    public static void authUserRequest(String userId,String password, NotificationListener listener) throws SQLException{
         connectToServer(); //etablir la connection avec le server centrale

        if (!isConnectedToServer) {
            System.err.println("[CLIENT] Not connected to central server.");
            //RECUPERER LES INFORMATIONS EN LOCAL
            String msg =  Database.authUser(userId, password);
            listener.onNotificationReceived(new Notification(msg.split("&&")[0], msg.split("&&")[1],5,"LOCALHOST")); // Notify listener with the response
            System.out.println("[CLIENT] Authentification response: "+msg.split("&&")[0] +" : "+msg.split("&&")[1]);
            return;
        }

        try {
            // Send request for user's address
            Message requestMessage = new Message("SERVER", "AUTHENTICATE_USER", userId+"&&"+password,"CENTRAL_SERVER");

            // Sérialiser en JSON et envoyer
            String jsonRequest = gson.toJson(requestMessage);
            System.out.println("[CLIENT] Sending JSON: " + jsonRequest);
            outCentral.write(jsonRequest +"\n");
            outCentral.flush();

            System.out.println("[CLIENT] Authentificate request starting for : " + userId);

            // Lire la réponse JSON

            String jsonResponse = inCentral.readLine();

            // Désérialiser en Notification
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null) {
                listener.onNotificationReceived(notification);
                System.out.println("[CLIENT] Authentification response: " + notification.getMessage());
            } else {
                System.err.println("[CLIENT] Unexpected response type: " + notification.getType());
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting authentificate: " + e.getMessage());
        }
    }

    public static void addUserRequest(String military_id, String password_hash, String grade, String division, String username ) throws SQLException,NoSuchAlgorithmException{
        if (!isConnectedToServer) {
            System.err.println("[CLIENT] Not connected to central server.");
            return;
        }

        try {
            // Send request for user's address
            Message requestMessage = new Message("SERVER", "SAVE_USER", military_id+"&&"+
                    password_hash+"&&"+grade+"&&"+division+"&&"+ "&&"+ username,"CENTRAL_SERVER");
            connectToServer();
            // Conversion en JSON
            String jsonRequest = gson.toJson(requestMessage);

            // Envoi via le flux texte
            System.out.println("[CLIENT] Sending JSON: " + jsonRequest);
            outCentral.write(jsonRequest +"\n");
            outCentral.flush();

            System.out.println("[CLIENT] Saving request starting for : " + military_id);

            // Lecture de la réponse JSON
            String jsonResponse = inCentral.readLine();
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null) {
                System.out.println("[CLIENT] SAVING response: " + notification.getMessage());
                if (notification.getMessage().equals("TRUE")){
                    //ENREGISTREMENT DE L'USER EN LOCAL
                    
                    User user = new User(military_id, grade, division, username);
                    user.setPasswordHash(password_hash);
                    Database.addUser(user);
                }

            } else {
                System.err.println("[CLIENT] Unexpected response type: " + notification.getClass().getName());
            }
//
        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting SAVE: " + e.getMessage());
        }
    }
    public static User getUserRequest(String military_id) throws SQLException{
        //METHODE PERMETTANT DE RECUPERER LES INFORMATINOS SUR L'USER
         if (!isConnectedToServer) {
             // VERIFICATION DE LA CONNEXION AVEC LA SERVEEUR CENTRAL
             
             System.err.println("[CLIENT] Not connected to central server.");
            return Database.getUser(military_id);
        }

        try {
            connectToServer();
            // Send request for user's address
            Message requestMessage = new Message("SERVER", "GET_USER", military_id,"CENTRAL_SERVER");
            //connectToServer();
            // Convert to JSON
            String jsonRequest = gson.toJson(requestMessage);

            // Send via text stream
            System.out.println("[CLIENT] Sending JSON: " + jsonRequest);
            outCentral.write(jsonRequest +"\n");
            outCentral.flush();

            System.out.println("[CLIENT] GETTING request starting for : " + military_id);

            // Read JSON response
            String jsonResponse = inCentral.readLine();
            System.out.println("Requete d'authentification envoyé , reception de reponse"+jsonResponse);
            // Parse JSON response into Notification
            Notification notification = gson.fromJson(jsonResponse, Notification.class);

            if (notification != null) {
                if (notification.getMessage() == null) {
                    System.out.println("Aucune notification reçu");
                    return null;
                }else{
                      User user = new User(notification.getMessage().split("&&")[0],
                                notification.getMessage().split("&&")[1],
                                notification.getMessage().split("&&")[2],
                                notification.getMessage().split("&&")[3]);
                      user.setPasswordHash(notification.getMessage().split("&&")[4]);
                    return user;
                }

            } else {
                System.err.println("[CLIENT] Unexpected response type: " + notification.getClass().getName());
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting GET: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
