package com.alaanya.alaanya;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import com.alaanya.database.DatabaseCentral;
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
                            System.out.println("AUTHENNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNTIFICATE");
                            String militaryId = message.getContent().split("&&")[0];
                            String password = message.getContent().split("&&")[1];

                            System.out.println("[CENTRAL SERVER] Authrnification to client: " +militaryId + " : " +clientSocket.getInetAddress());
                            userIPs.put(militaryId,clientSocket.getInetAddress().getHostAddress());
                            
                            
                            //cONNECTION A LA BD POUR VERIFIER LES INFOS
                            try (Connection conn = DatabaseCentral.getConnection()) {
                                String sql = "SELECT * FROM users WHERE military_id = ?";
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                stmt.setString(1, militaryId);

                                ResultSet rs = stmt.executeQuery();

                                if (rs.next()) {
                                    String storedPasswordHash = rs.getString("password_hash");

                                    if (storedPasswordHash.equals(password)) {
                                        out.writeObject(new Notification("AUTH_SUCCESS", "TRUE",5,"CENTRAL SERVER"));
                                        System.out.println("[CENTRAL SERVER] Authentication successful for user: " + militaryId);
                                    } else {
                                        out.writeObject(new Notification("AUTH_FAILURE", "MOT DE PASSE INCORRECT",5,"CENTRAL SERVER"));
                                        System.out.println("[CENTRAL SERVER] Authentication failed (wrong password) for user: " + militaryId);
                                    }
                                } else {
                                    out.writeObject(new Notification("AUTH_FAILURE", "IDENTIFIANT MILITAIRE INCORRECT.",5,"CENTRAL SERVER"));
                                    System.out.println("[CENTRAL SERVER] Authentication failed (user not found) for: " + militaryId);
                                }
                            } catch (Exception e) {
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
                            String militaryid = message.getContent().split("&&")[0];
                            String passwordHash = message.getContent().split("&&")[1];
                            String grade =  message.getContent().split("&&")[2];
                            String division =  message.getContent().split("&&")[3];
                            int clearanceLevel = Integer.parseInt(message.getContent().split("&&")[4]);
                            String username =  message.getContent().split("&&")[5];

                            String sql = "INSERT INTO users (military_id, password_hash, grade, division, clearance_level, username) VALUES (?, ?, ?, ?, ?, ?)";

                            try (Connection conn = DatabaseCentral.getConnection();
                                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                                
                                stmt.setString(1, militaryid);
                                stmt.setString(2, passwordHash);
                                stmt.setString(3, grade);
                                stmt.setString(4, division);
                                stmt.setInt(5, clearanceLevel);
                                stmt.setString(6, username);  // Ajout du nom d'utilisateur

                                stmt.executeUpdate();
                                out.writeObject(new Notification("SAVE_SUCCES", "TRUE",5,"CENTRAL SERVER")); //SIGNALE QUE TOUT C'EST BIEN PASSÉ
                                System.out.println("[CENTRAL SERVER] Authentication success (user found) for: " + militaryid);
                            }
                            catch (SQLException e) {
                                throw new SQLException("Erreur lors de l'enregistrement de l'utilisateur : " + e.getMessage());
                            }
                        }
                            
                        case "GET_USER" -> {
                            String militaryID = message.getContent();
                            String sql1 = "SELECT military_id, grade, division, clearance_level, username FROM users WHERE military_id = ?";
                            
                            try (Connection conn = DatabaseCentral.getConnection();
                                    PreparedStatement stmt = conn.prepareStatement(sql1)) {
                                
                                stmt.setString(1, militaryID);
                                ResultSet rs = stmt.executeQuery();

                                if (rs.next()) {
                                    //preparation du message (info du user)
                                    String msg =
                                            rs.getString("military_id")+"&&"+
                                            rs.getString("grade")+"&&"+
                                            rs.getString("division")+"&&"+
                                            rs.getInt("clearance_level")+"&&"+
                                            rs.getString("username");

                                    out.writeObject(new Notification("GET_SUCCES", msg,5,"CENTRAL SERVER"));
                                    System.out.println("[CENTRAL SERVER] getting success (user found) for: " + militaryID);
                                } else {
                                    System.out.println("[CENTRAL SERVER] getting FAILED (user not found) for: " + militaryID);
                                    out.writeObject(new Notification("GET_FAILED", null,5,"CENTRAL SERVER"));

                                }
                            }
                        }
                    }

                }

            } catch (IOException | ClassNotFoundException | SQLException e) {
                System.err.println("[CENTRAL SERVER] Error handling client: " + e.getMessage());
            }
        }

        private String getUserAddress(String requestedUserId) {
            return userIPs.get(requestedUserId);
        }
    }

    
}
