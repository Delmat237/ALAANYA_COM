package alaanya;

import file.FileReceicerThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;

public class Server extends ServerSocket {
    private static List<ClientHandler> clients = new ArrayList<>();
    public static String addressServer;

    static {
        try {
            addressServer = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public Server(int port) throws IOException, SQLException {
        super(port);

        //Creation de la BD du server

        Database.getConnection();


        while (true) {
            try {
                System.out.println("Server is running and waiting for connections...");

                Socket clientSocket = this.accept();
                InetAddress Nom_Address =  clientSocket.getInetAddress();
                String nomClient = Nom_Address.getHostName();
                String ClientId = Nom_Address.getHostAddress();


                ClientHandler clientHandler = new ClientHandler(clientSocket);

                System.out.println("New Client connected: ID = " + clientHandler.getClientId());
                //Save clients in db

                clientHandler.saveClientToDatabase(ClientId, nomClient);
                clients.add(clientHandler);
                clientHandler.start();
            } catch (IOException e) {
                System.err.println("Erreur lors de l'acceptation de la connexion : " + e.getMessage());
            }
        }
    }

    public static class Database {
        private static final String URL = "jdbc:mysql://localhost:3306/";
        private static final String USER = "delmat";
        private static final String PASSWORD = "azaleodel";

        public static Connection getConnection() throws SQLException {


            String databaseName = "Clients";
            String createDatabaseSQL = "CREATE DATABASE IF NOT EXISTS " + databaseName;
            String changeDatabaseSQL = "USE " + databaseName;

            String createTableSQLClients = """
                    CREATE TABLE IF NOT EXISTS clients(
                        id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                        client_id VARCHAR(25) NOT NULL,
                        client_name VARCHAR(25) 
                        ); 
                    """;
            String createTableSQLMessages = """
                
                CREATE TABLE IF NOT EXISTS messages (
                   id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                    sender_id VARCHAR(20) NOT NULL,
                    receiver_id VARCHAR(20) NOT NULL,
                    content TEXT,
                    file_path VARCHAR(255),
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    );
                """ ;
            try(Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement stmt = conn.createStatement();) {

                //Création des  bases de données

                stmt.execute(createDatabaseSQL);
                stmt.execute(changeDatabaseSQL);
                stmt.execute(createTableSQLClients);
                stmt.execute(createTableSQLMessages);

                return DriverManager.getConnection(URL+databaseName, USER, PASSWORD);

            } catch (SQLException e) {
                System.err.println("Erreur lors de la création de la base de données  ou des tables : " +e.getMessage());;
            }

            return null;
        }
    }



    public static void broadcastMessage(String msg, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(msg);
            }
        }
    }



    public static void sendMessageToClient(ClientHandler sender, String receiverId, String msg) {
        boolean flag = false;
        for (ClientHandler client : clients) {

            if (client.getClientId().equals(receiverId)) {

                client.sendMessage(sender.getClientId()+ "&&"+msg);
               
                flag = true;
                break;
            }
        }
        if (!flag) {
            sender.sendMessage(Server.addressServer+"&&Destinataire introuvable...");
        }
    }


    static class ClientHandler extends Thread {

        //public static final AtomicInteger idCounter = new AtomicInteger(0);
        private final String clientId;
        private Socket clientSocket;
        private DataOutputStream out;
        private DataInputStream in;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            //this.clientId = idCounter.incrementAndGet();
            this.clientId = socket.getInetAddress().getHostAddress();
            this.out = new DataOutputStream(this.clientSocket.getOutputStream());
            this.in = new DataInputStream(this.clientSocket.getInputStream());

        }

        @Override
        public void run() {
            try {
                out.writeUTF(Server.addressServer +"&& Bienvenue sur ALAANYA COM! Votre IP est " + clientId);
                //Signaler aux autres clients qu'il est online
                Server.broadcastMessage("STATUS&&"+clientId+"1&&",this);

                while (true) {
                    String messageType = in.readUTF(); //lIRE LE TYPE DE MESSAGE

                    // Format attendu : ID_EXPEDITEUR&&MESSAGE
                    String[] parts = messageType.split("&&");
                    String receiverId = parts[0];
                    String messageContent = parts[1];

                    System.out.println("[Client " + clientId + "] -> [" +receiverId +" ]: " + receiverId+" && "+messageContent);

                    Server.sendMessageToClient(this,receiverId,messageContent);
                    saveMessageToDatabase(this.clientId, receiverId, messageContent);
                }
            } catch (IOException e) {

                System.err.println("Erreur de communication avec le client " + clientId + " : " + e.getMessage());
                //SIGNALER AUX AUTRES CLIENTS QU'IL DECONNECTÉ
                Server.broadcastMessage("STATUS&&"+clientId+"0&&",this);
            } finally {
                try {
                    clients.remove(this);
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Erreur lors de la fermeture des ressources : " + e.getMessage());
                }
            }
        }


        public void sendMessage(String msg) {
            try {
                out.writeUTF(msg);
            } catch (IOException e) {
                System.err.println("Erreur d'envoi au client " + clientId + " : " + e.getMessage());
            }
        }


        private void saveClientToDatabase(String clientId, String client_name) {
            String sql = """
                            INSERT INTO clients (client_id, client_name) 
                            VALUES (?, ?) 
                          """;

            try (Connection conn = Database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, clientId);
                stmt.setString(2, client_name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Erreur lors de l'enregistrement du client : " + e.getMessage());
            }
        }

        private void saveMessageToDatabase(String senderId, String receiverId, String content) {
            String sql = "INSERT INTO Clients.messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";

            try (Connection conn = Database.getConnection()) {
                assert conn != null;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, senderId);
                    stmt.setString(2, receiverId);
                    stmt.setString(3, content);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors de l'enregistrement du message : " + e.getMessage());
            }
        }



        public String getClientId() {
            return clientId;
        }

    }

    public static void main(String[] args) {
        try {

            //LANCEMENT DES DIFFERENTS SOUS SERVEUR
            Server server = new Server(12345);
            AudioRelayServer ars = new AudioRelayServer(12346);
            FileRelayServer frl = new FileRelayServer(12347);



        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

