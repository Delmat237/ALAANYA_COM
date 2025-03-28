package com.alaanya.alaanya;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import com.alaanya.database.DatabaseCentral;
import com.alaanya.model.User;
import com.alaanya.socket.Message;
import com.alaanya.socket.Notification;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal"})

public class CentralServer {
    private static final int PORT = 8080;
    
    private static ConcurrentHashMap<String, String> userIPs = new ConcurrentHashMap<>(); // Maps user IDs to IP addresses

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[CENTRAL SERVER] Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[CENTRAL SERVER] Connected to client: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[CENTRAL SERVER] Error: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                Object request = in.readObject();

                if (request instanceof Message message) {
                    System.out.println("type de message reçu "+ message.getType());
                    switch (message.getType()) {
                        case "REQUEST_ADDRESS" -> {
                            String requestedUserId = message.getContent();
                            String userAddress = getUserAddress(requestedUserId);
                            if (userAddress != null) {
                                out.writeObject(new Notification("ADDRESS_RESPONSE", userAddress,5,"CENTRAL SERVER"));
                                System.out.println("[SERVER] Sent address for user: " + requestedUserId);
                            } else {
                                out.writeObject(new Notification("ADDRESS_NOT_FOUND", "Address not found for user: " + requestedUserId,5,"CENTRAL SERVER"));
                                System.out.println("[SERVER] Address not found for user: " + requestedUserId);
                            }
                        }
                        case "AUTHENTICATE_USER" -> {
                            System.out.println("AUTHENTIFICATE");
                            String militaryId = message.getContent().split("&&")[0];
                            String password = message.getContent().split("&&")[1];

                            System.out.println("[CENTRAL SERVER] Authrnification to client: " +militaryId + " : " +clientSocket.getInetAddress());
                            userIPs.put(militaryId,clientSocket.getInetAddress().getHostAddress());
                            
                            
                            try  {
                               String msg =  DatabaseCentral.authUser(militaryId, password);
                               out.writeObject(new Notification(msg.split("&&")[0], msg.split("&&")[1],5,"CENTRAL SERVER"));
                               
                            } catch (IOException | SQLException e) {
                                try {
                                    out.writeObject(new Notification("AUTH_ERROR", "Erreur lors de l'authentification.",5,"CENTRAL SERVER"));
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                                System.err.println("[CENTRAL SERVER] Error during authentication: " + e.getMessage());
                            }
                        }

                        case "SAVE_USER" -> {
                            System.out.println("SAVE USER BEGIN");

                            //recuperation des inforamtions
                          
                            User user = new User(
                                message.getContent().split("&&")[0],
                                message.getContent().split("&&")[2], 
                                message.getContent().split("&&")[3], 
                                Integer.parseInt(message.getContent().split("&&")[4]), 
                                message.getContent().split("&&")[5]);

                            user.setPasswordHash(message.getContent().split("&&")[1]);

                         try{
                                DatabaseCentral.addUser(user);

                                out.writeObject(new Notification("SAVE_SUCCES", "TRUE",5,"CENTRAL SERVER")); //SIGNALE QUE TOUT C'EST BIEN PASSÉ
                                System.out.println("[CENTRAL SERVER] Authentication success (user found) for: " + user.getMilitaryId());
                            }
                            catch (SQLException e) {
                                out.writeObject(new Notification("SAVE_FAILED", null,5,"CENTRAL SERVER"));
                                throw new SQLException("Erreur lors de l'enregistrement de l'utilisateur : " + e.getMessage());
                            }
                        }
                            
                        case "GET_USER" -> {
                            String militaryID = message.getContent();
                            
                            String msg = DatabaseCentral.getUser(militaryID);
                            if (msg != null)
                                out.writeObject(new Notification("GET_SUCCES", msg,5,"CENTRAL SERVER"));
                            else
                                out.writeObject(new Notification("GET_FAILED", null,5,"CENTRAL SERVER"));
                        }

                    }

                }

            } catch (IOException | ClassNotFoundException | SQLException e) {
                System.err.println("[CENTRAL SERVER] Error handling client: " + e.getMessage());
            } catch (NoSuchAlgorithmException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        private String getUserAddress(String requestedUserId) {
            return userIPs.get(requestedUserId);
        }
    }

    
}
