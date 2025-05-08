package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import server.database.DatabaseCentral;
import server.model.Message;
import server.model.Notification;
import server.model.User;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal"})

public class Main{
    private static final int PORT = 8080;
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();


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
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ) {

                String json = in.readLine();
                Message message = gson.fromJson(json, Message.class);

                if (message != null) {
                    System.out.println("type de message reçu "+ message.getType());
                    switch (message.getType()) {
                        case "REQUEST_ADDRESS" -> {
                            String requestedUserId = message.getContent();  //RECEIVE MESSSAGE 
                            String userAddress = DatabaseCentral.getHostAddress(requestedUserId); //SEARCH CURRENT USER  ADDRESS

                            if (userAddress != null) {
                                json = gson.toJson(new Notification("ADDRESS_RESPONSE", userAddress,5,"CENTRAL SERVER"));
                                out.write(json+"\n");
                                System.out.println("[SERVER] Sent address for user: " + requestedUserId);
                            } else {
                                json = gson.toJson(new Notification("ADDRESS_NOT_FOUND", "Address not found for user: " + requestedUserId,5,"CENTRAL SERVER"));
                                out.write(json+"\n");
                                System.out.println("[SERVER] Address not found for user: " + requestedUserId);
                            }
                        }
                        case "AUTHENTICATE_USER" -> {
                            System.out.println("AUTHENTIFICATE");
                            String militaryId = message.getContent().split("&&")[0];
                            String password = message.getContent().split("&&")[1];

                            System.out.println("[CENTRAL SERVER] Authrnification to client: " +militaryId + " : " +clientSocket.getInetAddress());
                            
                            try  {
                                //AUTHENTIFICATION DU USER
                               String msg =  DatabaseCentral.authUser(militaryId, password,clientSocket.getInetAddress().getHostAddress());
                                json = gson.toJson(new Notification(msg.split("&&")[0], msg.split("&&")[1],5,"CENTRAL SERVER"));
                                out.write(json+"\n");
                            } catch (IOException | SQLException e) {
                                try {
                                    json = gson.toJson(new Notification("AUTH_ERROR", "Erreur lors de l'authentification.",5,"CENTRAL SERVER"));
                                    out.write(json+"\n");
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
                          
                                message.getContent().split("&&")[5]);

                            user.setPasswordHash(message.getContent().split("&&")[1]);

                         try{
                                DatabaseCentral.addUser(user);
                                json = gson.toJson(new Notification("SAVE_SUCCES", "TRUE",5,"CENTRAL SERVER")); //SIGNALE QUE TOUT C'EST BIEN PASSÉ
                                out.write(json+"\n");
                                System.out.println("[CENTRAL SERVER] Authentication success (user found) for: " + user.getMilitaryId());
                            }
                            catch (SQLException e) {
                                json = gson.toJson(new Notification("SAVE_FAILED", null,5,"CENTRAL SERVER"));
                                out.write(json+"\n");
                                throw new SQLException("Erreur lors de l'enregistrement de l'utilisateur : " + e.getMessage());
                            }
                        }
                            
                        case "GET_USER" -> {
                            String militaryID = message.getContent();
                            
                            String msg = DatabaseCentral.getUser(militaryID);
                            if (msg != null) {
                                json = gson.toJson(new Notification("GET_SUCCES", msg, 5, "CENTRAL SERVER"));
                                out.write(json+"\n");
                            }
                            else{
                                json = gson.toJson(new Notification("GET_FAILED", null,5,"CENTRAL SERVER"));
                                out.write(json+"\n");
                            }
                        }

                    }

                }
                else {
                    json = gson.toJson(new Notification("ADDRESS_RESPONSE", "127.0.0.1",5,"CENTRAL SERVER"));
                    out.write(json+"\n");
                }

            } catch (IOException  | SQLException e) {
                System.err.println("[CENTRAL SERVER] Error handling client: " + e.getMessage());
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }


    }

    
}
