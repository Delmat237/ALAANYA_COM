package com.alaanya.socket;

import com.alaanya.database.Database;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

public class Server {

    private static final int PORT = 12345;
    private static final int NUM_THREADS = 10;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> userIPs = new HashMap<>();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Server started on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("[SERVER] Connected to client!, @IP Address : "+client.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(client);
                clients.add(clientHandler);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket client;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String militaryId;

        public ClientHandler(Socket client) {
            this.client = client;
        }

        private void sendMessage(Object message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("[SERVER] Error sending message: " + e.getMessage());
            }
        }


        private void receiveFile(FileMessage fileMessage) {
            String fileName = fileMessage.getFileName();
            byte[] fileData = fileMessage.getFileData();
            String sender = fileMessage.getSender();

            File directory = new File("received_files");
            if (!directory.exists()) {
                directory.mkdirs(); // Create the directory if it doesn't exist
            }

            File receivedFile = new File(directory, fileName);

            try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                fos.write(fileData);
                System.out.println("[SERVER] File saved: " + receivedFile.getAbsolutePath());

                sendFileToRecipient(fileMessage);

            } catch (IOException e) {
                System.err.println("[SERVER] Error saving file: " + e.getMessage());
            }
        }


        private void saveMessage(Message message) {
            String sql = "INSERT INTO messages (sender, recipient, content) VALUES (?, ?, ?)";

            try (Connection conn =Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, message.getSender());
                stmt.setString(2, message.getRecipient());
                stmt.setString(3, message.getContent());
                System.out.println("msg : "+ message.getContent());
                stmt.executeUpdate();
                System.out.println("[SERVER] Message saved to database.");

            } catch (SQLException e) {
                System.err.println("[SERVER] Error saving message to database: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void addContactToDatabase(String userId, String contactId) {
            String sql = "INSERT INTO contacts (user_id, contact_id) VALUES (?, ?)";

            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, userId);
                stmt.setString(2, contactId);

                stmt.executeUpdate();
                System.out.println("[SERVER] Contact ajouté : " + userId + " -> " + contactId);

            } catch (SQLException e) {
                System.err.println("[SERVER] Erreur lors de l'ajout du contact : " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void sendMessageToRecipient(Message message) {
            String recipient = message.getRecipient();

            for (ClientHandler clientHandler : clients) {
                if (clientHandler.militaryId != null && clientHandler.militaryId.equals(recipient)) {
                    clientHandler.sendMessage(message);
                    System.out.println("[SERVER] Message sent to recipient: " + recipient);
                    return;
                }
            }

            System.out.println("[SERVER] Recipient not found: " + recipient);
        }
        private void sendFileToRecipient(FileMessage fileMessage) {
            String recipientId = fileMessage.getRecipient();
            // Find the recipient's ClientHandler
            ClientHandler recipientHandler = findClientHandler(recipientId);

            if (recipientHandler != null) {
                // Send the file message to the recipient
                recipientHandler.sendMessage(fileMessage);
                System.out.println("[SERVER] File sent to recipient: " + recipientId);
            } else {
                System.out.println("[SERVER] Recipient not found: " + recipientId);
            }
        }

        private ClientHandler findClientHandler(String recipientId) {
            for (ClientHandler clientHandler : clients) {
                if (clientHandler.militaryId != null && clientHandler.militaryId.equals(recipientId)) {
                    return clientHandler;
                }
            }
            return null;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(client.getOutputStream());
                in = new ObjectInputStream(client.getInputStream());

                while (true) {
                    Object request = in.readObject();

                    if (request instanceof Message) {
                        Message message = (Message) request;
                        System.out.println("[SERVER] Received message: " + message.getContent() +
                                " from: " + message.getSender());
                        System.out.println(message.getType());

                        switch (message.getType()) {
                            case "MESSAGE":
                                // Enregistrement dans la base de données (Section 4 du cahier des charges)
                                saveMessage(message);

                                // Envoi au destinataire spécifique
                                sendMessageToRecipient(message);
                                break;
                            case "ADD_CONTACT":
                                // Ajouter un contact dans la base de données
                                String[] contactInfo = message.getContent().split(":");
                                String userId = contactInfo[0];
                                String contactId = contactInfo[1];
                                addContactToDatabase(userId, contactId);
                                break;
                            case "FILE_UPLOAD":
                                if (message instanceof FileMessage) {
                                    FileMessage fileMessage = (FileMessage) message;

                                    // Enregistrement dans la base de données (Section 4 du cahier des charges)
                                    saveMessage(fileMessage);
                                } else {
                                    System.err.println("[SERVER] Expected FileMessage but received Message");
                                }
                                break;
                        }
                    } else if (request == null) {
                        break;
                    } else {
                        System.out.println("[SERVER] Received unknown request: " + request);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error in ClientHandler: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(this);
                System.out.println("[SERVER] Client disconnected.");
            }
        }
    }
}
