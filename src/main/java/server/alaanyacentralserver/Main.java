package server.alaanyacentralserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import server.database.DatabaseCentral;
import server.model.Message;
import server.model.Notification;
import server.model.User;

@SuppressWarnings({"CallToPrintStackTrace", "FieldMayBeFinal"})

public class Main {
    // Port d'écoute du serveur central
    private static final int PORT = 7000;

    // Instance Gson pour (dé)sérialiser les objets JSON
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

    // Logger pour journaliser les activités du serveur
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Création du socket serveur
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("[CENTRAL SERVER] Server is running on port " + PORT);

            // Boucle infinie : accepte continuellement les connexions entrantes
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("[CENTRAL SERVER] Connected to client: " + clientSocket.getInetAddress());

                // Délègue le traitement du client à un thread séparé
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[CENTRAL SERVER] Server error: ", e);
        }
    }

    // Classe interne pour gérer chaque client connecté
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
            ) {
                //clientSocket.setSoTimeout(10000); // Timeout pour éviter les connexions mortes

                String jsonRequest;

                while ((jsonRequest = in.readLine()) != null) {
                    if (jsonRequest.trim().isEmpty()) {
                        logger.warning("[SERVER] Requête vide reçue.");
                        continue;
                    }

                    logger.info("[SERVER] :JSON REQUEST: " + jsonRequest);

                    // Désérialisation en objet Message
                    Message message = gson.fromJson(jsonRequest, Message.class);
                    String type = message.getType();

                    logger.info("Type de message reçu : " + type);

                    // Traitement de la requête
                    switch (type) {
                        case "REQUEST_ADDRESS" -> handleRequestAddress(out, message);
                        case "AUTHENTICATE_USER" -> handleAuthenticateUser(out, message);
                        case "SAVE_USER" -> handleSaveUser(out, message);
                        case "GET_USER" -> handleGetUser(out, message);
                        default -> {
                            logger.warning("[SERVER] Type de message inconnu : " + type);
                            sendJsonResponse(out, new Notification("ERROR", "Type de message inconnu", 5, "CENTRAL SERVER"));
                        }
                    }
                }

                logger.info("[CENTRAL SERVER] Le client a fermé la connexion proprement.");

            } catch (IOException e) {
                logger.log(Level.SEVERE, "[CENTRAL SERVER] Erreur lors du traitement client : ", e);
            } finally {
                try {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                        logger.info("[CENTRAL SERVER] Connexion client fermée.");
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "[CENTRAL SERVER] Erreur fermeture socket : ", e);
                }
            }
        }

        // Gère la requête de récupération d’adresse IP d’un utilisateur
        private void handleRequestAddress(BufferedWriter out, Message message) throws IOException {
            String requestedUserId = message.getContent();
            String userAddress = null;
            try {
                userAddress = DatabaseCentral.getHostAddress(requestedUserId);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[CENTRAL SERVER] Erreur lors de la récupération de l'adresse : ", e);
            }

            Notification response = (userAddress != null)
                ? new Notification("ADDRESS_RESPONSE", userAddress, 5, "CENTRAL SERVER")
                : new Notification("ADDRESS_NOT_FOUND", "Adresse non trouvée pour utilisateur : " + requestedUserId, 5, "CENTRAL SERVER");

            logger.info("[SERVER] Adresse " + (userAddress != null ? "envoyée" : "non trouvée") + " pour : " + requestedUserId);
            sendJsonResponse(out, response);
        }

        // Gère la requête d’authentification
        private void handleAuthenticateUser(BufferedWriter out, Message message) throws IOException {
            logger.info("AUTHENTIFICATION");

            // Les credentials sont encodés comme militaryId&&password
            String[] parts = message.getContent().split("&&");

            if (parts.length < 2) {
                sendJsonResponse(out, new Notification("INVALID_REQUEST", "Paramètres d'authentification invalides", 5, "CENTRAL SERVER"));
                return;
            }

            String militaryId = parts[0];
            String password = parts[1];
            logger.info("[CENTRAL SERVER] Authentification de : " + militaryId + " depuis " + clientSocket.getInetAddress());

            try {
                // Tente l’authentification en base
                String msg = DatabaseCentral.authUser(militaryId, password, clientSocket.getInetAddress().getHostAddress());
                String[] res = msg.split("&&"); // Format attendu : TYPE&&MESSAGE
                sendJsonResponse(out, new Notification(res[0], res[1], 5, "CENTRAL SERVER"));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[CENTRAL SERVER] Erreur lors de l'authentification : ", e);
                sendJsonResponse(out, new Notification("AUTH_ERROR", "Erreur lors de l'authentification.", 5, "CENTRAL SERVER"));
            }
        }

        // Gère l’enregistrement d’un nouvel utilisateur
        private void handleSaveUser(BufferedWriter out, Message message) throws IOException {
            logger.info("SAVE USER BEGIN");

            // Format : militaryId&&passwordHash&&nom&&grade&&unité
            String[] parts = message.getContent().split("&&");

            if (parts.length < 5) {
                sendJsonResponse(out, new Notification("INVALID_REQUEST", "Paramètres d'utilisateur invalides", 5, "CENTRAL SERVER"));
                return;
            }

            // Création de l’objet User
            User user = new User(parts[0], parts[2], parts[3], parts[4]);

            try {
                // Hachage du mot de passe
                user.setPasswordHash(parts[1]);
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, "[CENTRAL SERVER] Erreur lors du hachage du mot de passe : ", e);
                sendJsonResponse(out, new Notification("HASH_ERROR", "Erreur lors du hachage du mot de passe.", 5, "CENTRAL SERVER"));
                return;
            }

            try {
                // Enregistrement dans la base de données centrale
                DatabaseCentral.addUser(user);
                logger.info("[CENTRAL SERVER] Utilisateur enregistré : " + user.getMilitaryId());
                sendJsonResponse(out, new Notification("SAVE_SUCCESS", "TRUE", 5, "CENTRAL SERVER"));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[CENTRAL SERVER] Erreur lors de l'enregistrement : ", e);
                sendJsonResponse(out, new Notification("SAVE_FAILED", "FALSE", 5, "CENTRAL SERVER"));
            }
        }

        // Gère la récupération des informations d’un utilisateur
        private void handleGetUser(BufferedWriter out, Message message) throws IOException {
            logger.info(" STARTED GETTING USER : "+message.getContent());

            String militaryID = message.getContent();
            String msg = null;

            try {
                // Requête SQL pour récupérer les infos de l’utilisateur
                msg = DatabaseCentral.getUser(militaryID);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[CENTRAL SERVER] Erreur lors de la récupération de l'utilisateur : ", e);
                sendJsonResponse(out, new Notification("GET_FAILED", "Erreur lors de la récupération de l'utilisateur.", 5, "CENTRAL SERVER"));
                return;
            }

            Notification response = (msg != null)
                ? new Notification("GET_SUCCESS", msg, 5, "CENTRAL SERVER")
                : new Notification("GET_FAILED", null, 5, "CENTRAL SERVER");

            sendJsonResponse(out, response);
        }

        // Méthode utilitaire pour envoyer une réponse JSON
        private void sendJsonResponse(BufferedWriter out, Notification notification) throws IOException {
            out.write(gson.toJson(notification) +"\n");
            out.flush();
        }
    }
}
