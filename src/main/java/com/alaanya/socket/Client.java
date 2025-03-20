package com.alaanya.socket;

import com.alaanya.model.User;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import javafx.stage.FileChooser;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

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



//    public static void sendMessageToServer(Message message){
//        if (!isConnectedToServer) {
//            System.err.println("[CLIENT] Not connected to central server.  Attempting to reconnect.");
//            if (!connectToServer()) {
//                System.err.println("[CLIENT] Reconnection failed. Message not sent.");
//                return;
//            }
//        }
//        try {
//            outCentral.writeObject(message);
//            outCentral.flush();
//            System.out.println("[CLIENT] Message sent: " + message.getContent());
//        } catch (SocketException se) {
//            System.err.println("[CLIENT] SocketException while sending. Reconnecting..." + se.getMessage());
//            isConnectedToServer = false;
//            if (!connectToServer()) {
//                System.err.println("[CLIENT] Reconnection failed. Message not sent.");
//            } else {
//                sendMessageToServer(message); // Try sending again after reconnecting
//            }
//        } catch (IOException e) {
//            System.err.println("[CLIENT] Error sending message: " + e.getMessage());
//            isConnected = false;
//        }
//    }
    public static void sendMessage(Message message, String recipientAddress) {
        if (!isConnected) {
            System.err.println("[CLIENT] Not connected to server recipient.  Attempting to reconnect.");
            if (!connect(recipientAddress)) {
                System.err.println("[CLIENT] Reconnection failed. Message not sent.");
                return;
            }
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

//    private static void startCentralReceiving() {
//        Thread receiveThread = new Thread(() -> {
//            try {
//                while (isConnectedToServer) {
//                    try {
//                        Object receivedObject = inCentral.readObject();
//
//                        if (receivedObject == null) {
//                            System.err.println("[CLIENT] Received null object.  Connection may be closed.");
//                            isConnectedToServer = false;
//                            break;
//                        }
//
//                        if (receivedObject instanceof Message) {
//                            Message receivedMessage = (Message) receivedObject;
//                            System.out.println("[CLIENT] Received message: " + receivedMessage.getContent() + " from " + receivedMessage.getSender());
//
//                        } else {
//                            System.err.println("[CLIENT] Received unknown object type: " + receivedObject.getClass().getName());
//                        }
//
//                    } catch (ClassNotFoundException e) {
//                        System.err.println("[CLIENT] ClassNotFoundException: " + e.getMessage());
//                        isConnectedToServer = false;
//                        break;
//                    } catch (SocketException e) {
//                        System.err.println("[CLIENT] SocketException while receiving: " + e.getMessage());
//                        isConnectedToServer = false;
//                        break;
//                    } catch (IOException e) {
//                        System.err.println("[CLIENT] IOException while receiving: " + e.getMessage());
//                        isConnectedToServer = false;
//                        break;
//                    }
//                }
//            } finally {
//                closeConnection();
//            }
//        });
//        receiveThread.setDaemon(true);
//        receiveThread.start();
//    }
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

                        if (receivedObject instanceof Message) {
                            Message receivedMessage = (Message) receivedObject;
                            System.out.println("[CLIENT] Received message: " + receivedMessage.getContent() + " from " + receivedMessage.getSender());

                            Platform.runLater(() -> {
                                // Code to access and update UI
                                System.out.println("[CLIENT] Message Received");
                            });
                        } else if (receivedObject instanceof Notification) {
                            Notification notification = (Notification) receivedObject;
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
            if (response instanceof Notification) {
                Notification notification = (Notification) response;
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
    public static void authUserRequest(String userId,String password, NotificationListener listener) {
        if (!isConnectedToServer) {
            System.err.println("[CLIENT] Not connected to central server.");
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
                System.out.println("[CLIENT] Authentificate response: " + notification.getMessage());
            } else {
                System.err.println("[CLIENT] Unexpected response type: " + response.getClass().getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[CLIENT] Error requesting authentificate: " + e.getMessage());
        }
    }

    public static void addUserRequest(String military_id, String password_hash, String grade, String division, int clearance_level, String username ){
        if (!isConnectedToServer) {
            System.err.println("[CLIENT] Not connected to central server.");
            return;
        }

        try {
            // Send request for user's address
            Message requestMessage = new Message("SERVER", "SAVE_USER", military_id+"&&"+
                    password_hash+"&&"+grade+"&&"+division+"&&"+ clearance_level+"&&"+ username);
            connectToServer();
            outCentral.writeObject(requestMessage);
            outCentral.flush();
            System.out.println("[CLIENT] Saving request starting for : " + military_id);

            // Read response
            Object response = inCentral.readObject();
//
        } catch (IOException e) {
            System.err.println("[CLIENT] Error requesting SAVE: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static User getUserRequest(String military_id){
        //METHODE PERMETTANT DE RECUPERER LES INFORMATINOS SUR L'USER
        if (!isConnectedToServer) {
            // VERIFICATION DE LA CONNEXION AVEC LA SERVEEUR CENTRAL
            System.err.println("[CLIENT] Not connected to central server.");
            return null;
        }

        try {
            // Send request for user's address
            Message requestMessage = new Message("SERVER", "GET_USER", military_id);
            //connectToServer();
            outCentral.writeObject(requestMessage);
            outCentral.flush();
            System.out.println("[CLIENT] GETTING request starting for : " + military_id);

            // Read response
            Object response = inCentral.readObject();
            System.out.println("Requete d'authentification envoyé , reception de reponse");
            if (response instanceof Notification) {

                if (((Notification) response).getMessage() == null) {
                    System.out.println("Aucune notification reçu");
                    return null;
                }else{
                    return new User(((Notification) response).getMessage().split("&&")[0],
                            ((Notification) response).getMessage().split("&&")[1],
                            ((Notification) response).getMessage().split("&&")[2],
                            Integer.parseInt(((Notification) response).getMessage().split("&&")[3]),
                            ((Notification) response).getMessage().split("&&")[4]);
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
