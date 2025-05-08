package server.alaanyacentralserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import server.database.DatabaseCentral;
import server.model.Message;
import server.model.User;
import server.model.Notification;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal"})

public class Main{
    private static final int PORT = 8080;
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

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
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

                // Lecture de la requête JSON (une ligne)
                String jsonRequest = in.readLine();
                System.out.println(jsonRequest);
                if (jsonRequest == null) {
                    System.err.println("[SERVER] Requête vide reçue.");
                    return;
                }

                // Désérialisation en Message
                Message message = gson.fromJson(jsonRequest, Message.class);

                System.out.println("Type de message reçu : " + message.getType());

                switch (message.getType()) {
                    case "REQUEST_ADDRESS" -> {
                        String requestedUserId = message.getContent();
                        String userAddress = DatabaseCentral.getHostAddress(requestedUserId);

                        Notification response;
                        if (userAddress != null) {
                            response = new Notification("ADDRESS_RESPONSE", userAddress, 5, "CENTRAL SERVER");
                            System.out.println("[SERVER] Adresse envoyée pour utilisateur : " + requestedUserId);
                        } else {
                            response = new Notification("ADDRESS_NOT_FOUND", "Adresse non trouvée pour utilisateur : " + requestedUserId, 5, "CENTRAL SERVER");
                            System.out.println("[SERVER] Adresse non trouvée pour utilisateur : " + requestedUserId);
                        }

                        // Envoi de la réponse JSON
                        sendJsonResponse(out, response);
                    }
                    case "AUTHENTICATE_USER" -> {
                        System.out.println("AUTHENTIFICATION");

                        String[] parts = message.getContent().split("&&");
                        String militaryId = parts[0];
                        String password = parts[1];

                        System.out.println("[CENTRAL SERVER] Authentification de : " + militaryId + " depuis " + clientSocket.getInetAddress());

                        try {
                            String msg = DatabaseCentral.authUser(militaryId, password, clientSocket.getInetAddress().getHostAddress());
                            Notification response = new Notification(msg.split("&&")[0], msg.split("&&")[1], 5, "CENTRAL SERVER");
                            sendJsonResponse(out, response);
                        } catch (IOException | SQLException e) {
                            Notification errorResponse = new Notification("AUTH_ERROR", "Erreur lors de l'authentification.", 5, "CENTRAL SERVER");
                            sendJsonResponse(out, errorResponse);
                            System.err.println("[CENTRAL SERVER] Erreur lors de l'authentification : " + e.getMessage());
                        }
                    }
                    case "SAVE_USER" -> {
                        System.out.println("SAVE USER BEGIN");

                        String[] parts = message.getContent().split("&&");
                        User user = new User(parts[0], parts[2], parts[3], parts[4]);
                        user.setPasswordHash(parts[1]);

                        try {
                            DatabaseCentral.addUser(user);
                            Notification successResponse = new Notification("SAVE_SUCCESS", "TRUE", 5, "CENTRAL SERVER");
                            sendJsonResponse(out, successResponse);
                            System.out.println("[CENTRAL SERVER] Utilisateur enregistré : " + user.getMilitaryId());
                        } catch (SQLException e) {
                            Notification failResponse = new Notification("SAVE_FAILED", null, 5, "CENTRAL SERVER");
                            sendJsonResponse(out, failResponse);
                            System.err.println("[CENTRAL SERVER] Erreur lors de l'enregistrement : " + e.getMessage());
                        }
                    }
                    case "GET_USER" -> {
                        String militaryID = message.getContent();
                        String msg = DatabaseCentral.getUser(militaryID);

                        Notification response;
                        if (msg != null) {
                            response = new Notification("GET_SUCCESS", msg, 5, "CENTRAL SERVER");
                        } else {
                            response = new Notification("GET_FAILED", null, 5, "CENTRAL SERVER");
                        }
                        sendJsonResponse(out, response);
                    }
                    default -> {
                        System.err.println("[SERVER] Type de message inconnu : " + message.getType());
                        Notification errorResponse = new Notification("ERROR", "Type de message inconnu", 5, "CENTRAL SERVER");
                        sendJsonResponse(out, errorResponse);
                    }
                }

            } catch (IOException | SQLException | NoSuchAlgorithmException e) {
                System.err.println("[CENTRAL SERVER] Erreur lors du traitement client : " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void sendJsonResponse(BufferedWriter out, Notification notification) throws IOException {
            String jsonResponse = gson.toJson(notification);
            out.write(jsonResponse);
            out.newLine(); // Important pour délimiter le message
            out.flush();
        }
    }



    
}
