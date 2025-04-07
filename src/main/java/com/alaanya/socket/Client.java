package com.alaanya.socket;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import com.alaanya.database.Database;
import com.alaanya.model.User;

import javafx.application.Platform;
import javafx.stage.FileChooser;


@SuppressWarnings({"CallToPrintStackTrace","FieldMayBeFinal"})

public class Client {

    private static final int SERVER_PORT = 12345;
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectOutputStream outCentral;
    private static ObjectInputStream in;
    private static ObjectInputStream inCentral;
    private static boolean isConnected = false;
    private static boolean isConnectedToServer = false;
    public static String userId;

    private static final String CENTRAL_SERVER_IP = "127.0.0.1";
    private static final int CENTRAL_SERVER_PORT = 8080;

    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
    }

    private static NotificationListener notificationListener;

    public static boolean connect(String recipientAddress) {
        //Connection au serveur d'un client pour echanger
        try {
            socket = new Socket(recipientAddress, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            isConnected = true;
            System.out.println("[CLIENT] Connected to server");
            startReceiving(); // Start listening for messages immediately after connecting
            return true;
        } catch (IOException e) {
            System.err.println("[CLIENT] Error connecting to server: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    //Connection au serveur centrale
    public static boolean connectToServer(){
        try {
            @SuppressWarnings("resource")
            Socket socketCentral = new Socket(CENTRAL_SERVER_IP, CENTRAL_SERVER_PORT);
            outCentral = new ObjectOutputStream(socketCentral.getOutputStream());
            inCentral = new ObjectInputStream(socketCentral.getInputStream());

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
     * message : Message tha we want to send 
     * recipientAddress : Current address of receiver
     */
    public static void sendMessage(Message message, String recipientAddress) {
        if (!isConnected) {
            System.err.println("[CLIENT] Not connected to server recipient.  Attempting to reconnect.");
            if (!connect(recipientAddress)) {
                System.err.println("[CLIENT] Reconnection failed. Message not sent.");
                return;
            }

            //sI LE CLIENT N'EST PAS ONLINE LE MESSAGE N'EST PAS ENVOYÉ
        }

        try {
            out.writeObject(message);
            out.flush();
            System.out.println("[CLIENT] Message sent: " + message.getContent());
        } catch (SocketException se) {
            System.err.println("[CLIENT] SocketException while sending. Reconnecting..." + se.getMessage());
            isConnected = false;
            if (!connect(recipientAddress)) {
                System.err.println("[CLIENT] Reconnection failed. Message not sent.");
            } else {
                sendMessage(message,recipientAddress); // Try sending again after reconnecting
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error sending message: " + e.getMessage());
            isConnected = false;
        }
    }



    // Method to select a file and send it
    public void sendFile(String sender,String recipient, String recipientAddress) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());
                String fileName = selectedFile.getName();
                FileMessage fileMessage = new FileMessage(sender, "FILE_UPLOAD", "File Upload", fileName, fileData,recipient);
                Client.sendMessage(fileMessage,recipientAddress); // Use your sendMessage method to send the file
                System.out.println("[CLIENT] Sending file: " + fileName + " (" + fileData.length + " bytes)");
            } catch (IOException e) {
                System.err.println("[CLIENT] Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("[CLIENT] File selection cancelled.");
        }
    }

    private static void startReceiving() {
        Thread receiveThread = new Thread(() -> {
            try {
                while (isConnected) {
                    try {
                        Object receivedObject = in.readObject();

                        if (receivedObject == null) {
                            System.err.println("[CLIENT] Received null object.  Connection may be closed.");
                            isConnected = false;
                            break;
                        }

                        if (receivedObject instanceof Message receivedMessage) {
                            System.out.println("[CLIENT] Received message: " + receivedMessage.getContent() + " from " + receivedMessage.getSender());

                            Platform.runLater(() -> {
                                // Code to access and update UI
                                System.out.println("[CLIENT] Message Received");
                            });
                        } else if (receivedObject instanceof Notification notification) {
                            System.out.println("[CLIENT] Received notification: " + notification.getMessage() + " type " + notification.getType());

                            if (notificationListener != null) {
                                notificationListener.onNotificationReceived(notification);
                            }
                        } else {
                            System.err.println("[CLIENT] Received unknown object type: " + receivedObject.getClass().getName());
                        }

                    } catch (ClassNotFoundException e) {
                        System.err.println("[CLIENT] ClassNotFoundException: " + e.getMessage());
                        isConnected = false;
                        break;
                    } catch (SocketException e) {
                        System.err.println("[CLIENT] SocketException while receiving: " + e.getMessage());
                        isConnected = false;
                        break;
                    } catch (IOException e) {
                        System.err.println("[CLIENT] IOException while receiving: " + e.getMessage());
                        isConnected = false;
                        break;
                    }
                }
            } finally {
                closeConnection();
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    public static void closeConnection() {
        isConnected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            System.out.println("[CLIENT] Connection closed");
        } catch (IOException e) {
            System.err.println("[CLIENT] Error closing connection: " + e.getMessage());
        }
    }

    public static boolean isConnected() {
        return isConnected;
    }
    public static boolean isIsConnectedToServer() {
        return isConnectedToServer;
    }

    public static void setNotificationListener(NotificationListener listener) {
        notificationListener = listener;
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
            Message requestMessage = new Message("SERVER", "REQUEST_ADDRESS", userId);
            System.out.println("le message est constitué");
            connectToServer();
            outCentral.writeObject(requestMessage);
            outCentral.flush();
            System.out.println("[CLIENT] Address request sent for user: " + userId);

            // Read response
            Object response = inCentral.readObject();
            if (response instanceof Notification notification) {
                listener.onNotificationReceived(notification); // Notify listener with the response
                System.out.println("[CLIENT] Received address response: " + notification.getMessage());
            } else {
                System.err.println("[CLIENT] Unexpected response type: " + response.getClass().getName());
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting Address: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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
            Message requestMessage = new Message("SERVER", "AUTHENTICATE_USER", userId+"&&"+password);
            //connectToServer();
            outCentral.writeObject(requestMessage);
            outCentral.flush();
            System.out.println("[CLIENT] Authentificate request starting for : " + userId);

            // Read response
            Object response = inCentral.readObject();

            if (response instanceof Notification notification) {
                listener.onNotificationReceived(notification); // Notify listener with the response
                System.out.println("[CLIENT] Authentification response: " + notification.getMessage());

                 //start server 
                new Thread(() -> {
                    System.out.println("Server started :");
                new Server().main(null);
                }).start();
            } else {
                System.err.println("[CLIENT] Unexpected response type: " + response.getClass().getName());
            }

        } catch (IOException | ClassNotFoundException e) {
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
                    password_hash+"&&"+grade+"&&"+division+"&&"+ "&&"+ username);
            connectToServer();
            outCentral.writeObject(requestMessage);
            outCentral.flush();
            System.out.println("[CLIENT] Saving request starting for : " + military_id);

            // Read response
            Object response = inCentral.readObject();
            if (response instanceof Notification notification) {
               
                System.out.println("[CLIENT] SAVING response: " + notification.getMessage());
                if (notification.getMessage().equals("TRUE")){
                    //ENREGISTREMENT DE L'USER EN LOCAL
                    
                    User user = new User(military_id, grade, division, username);
                    user.setPasswordHash(password_hash);
                    Database.addUser(user);
                }

            } else {
                System.err.println("[CLIENT] Unexpected response type: " + response.getClass().getName());
            }
//
        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting SAVE: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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
            Message requestMessage = new Message("SERVER", "GET_USER", military_id);
            //connectToServer();
            outCentral.writeObject(requestMessage);
            outCentral.flush();
            System.out.println("[CLIENT] GETTING request starting for : " + military_id);

            // Read response
            Object response = inCentral.readObject();
            System.out.println("Requete d'authentification envoyé , reception de reponse");
            if (response instanceof Notification notification) {

                if (notification.getMessage() == null) {
                    System.out.println("Aucune notification reçu");
                    return null;
                }else{
                    return new User(notification.getMessage().split("&&")[0],
                            notification.getMessage().split("&&")[1],
                            notification.getMessage().split("&&")[2],
                            notification.getMessage().split("&&")[3]);
                }

            } else {
                System.err.println("[CLIENT] Unexpected response type: " + response.getClass().getName());
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting GET: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
