package database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javafx.scene.image.Image;
import model.Contact;
import model.Message;
import model.User;

public class Database {
    private static final String DB_PROPERTIES_FILE = "dbSqlite.properties";
    private static String URL;
  

    static {
            Properties props = new Properties();
            InputStream input = null;
            try {
                input = Database.class.getClassLoader().getResourceAsStream(DB_PROPERTIES_FILE);
                if (input == null) {
                    System.err.println("Unable to find " + DB_PROPERTIES_FILE);
                    throw new IOException("Unable to find " + DB_PROPERTIES_FILE);
                }
                props.load(input);
                URL = props.getProperty("db.url");

            } catch (IOException ex) {
                System.err.println("Erreur lors du chargement du fichier de configuration : " + ex.getMessage());
                ex.printStackTrace();
                throw new RuntimeException("Erreur lors du chargement du fichier de configuration", ex);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                         System.out.println(e.getMessage() +" : " +e.getCause());
                    }
                }
            }

            // Créer la base de données et la table si elles n'existent pas
            try {
                createDatabaseAndTable();
            } catch (SQLException e) {
                System.err.println("Erreur lors de la création de la base de données et de la table : " + e.getMessage());
                 System.out.println(e.getMessage() +" : " +e.getCause());
                throw new RuntimeException("Erreur lors de la création de la base de données et de la table", e);
            }
        }

    public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL);
        }

    private static void createDatabaseAndTable() throws SQLException {
            try (Connection conn = DriverManager.getConnection(URL);
                 Statement stmt = conn.createStatement()) {

                // Création table users
                stmt.executeUpdate("""
                     CREATE TABLE IF NOT EXISTS users (
                             id INT AUTO_INCREMENT PRIMARY KEY,
                             phone_Number VARCHAR(15) NOT NULL,
                             password_hash VARCHAR(64) NOT NULL,
                             grade VARCHAR(50) NOT NULL,
                             division VARCHAR(50) NOT NULL,
                             profilePicture TEXT DEFAULT "/com/alaanya/view/images/profile.png",
                             username VARCHAR(255) NOT NULL
                         );""");

                // Création table contacts
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS contacts (
                        id  INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id VARCHAR(15) NOT NULL,
                        contact_user_id VARCHAR(15) UNIQUE  NOT NULL,
                        nick_name TEXT NOT NULL,
                        lastMessage TEXT ,
                        statut INTEGER CHECK(statut IN (0, 1)) DEFAULT 0,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(phone_Number),
                        UNIQUE(user_id, contact_user_id)
                    );""");

                // Création table messages
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sender_id VARCHAR(15) NOT NULL,
                        receiver_id VARCHAR(15) NOT NULL,
                        type VARCHAR(10) NOT NULL DEFAULT "MESSAGE",
                        content TEXT ,
                        fileName TEXT,
                        filePath TEXT,
                        timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
                        ack TEXT CHECK(ack IN ('sent', 'receive')) DEFAULT 'sent',
                        statut TEXT CHECK (statut IN ('read','notread','delivered')) DEFAULT 'delivered',
                        FOREIGN KEY (sender_id) REFERENCES users(phone_Number)
                      \s
                    );
                   \s""");
            }
        }


    @SuppressWarnings("CallToPrintStackTrace")
     public static void addContact(String userId,String contact_user_id,int statut,String nick_name){
        String sql = "INSERT INTO contacts (user_id, contact_user_id,statut,nick_name) VALUES (?, ?, ? ,?)";
            //aPRES je vais retirer ca ici (Factory)
            try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

           stmt.setString(1, userId);
           stmt.setString(2, contact_user_id);
           stmt.setLong(3, statut);
           stmt.setString(4,nick_name);

           stmt.executeUpdate();
           System.out.println("[SERVER] Contact added: " + userId + " -> " + contact_user_id);

           // Update the contact list
           //loadContacts();

       } catch (SQLException e) {
           System.err.println("[SERVER] Error adding contact: " + e.getMessage());
            System.out.println(e.getMessage() +" : " +e.getCause());
       }
    }

     public static String authUser(String militaryId, String password) throws SQLException{
                Connection conn = Database.getConnection();

                String sql = "SELECT * FROM users WHERE phone_Number= ?";
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 stmt.setString(1, militaryId);

              ResultSet rs = stmt.executeQuery();

                  if (rs.next()) {
                                        String storedPasswordHash = rs.getString("password_hash");

                      if (storedPasswordHash.equals(password)) {
                                       System.out.println("[LOCALHOST] Authentication successful for user: " + militaryId);
                                        return "AUTH_SUCCESS&&TRUE";

                    } else {
                                        System.out.println("[LOCALHOST] Authentication failed (wrong password) for user: " + militaryId);
                                         return "AUTH_FAILURE&&MOT DE PASSE INCORRECT";
                     }
               } else {
                            System.out.println("[LOCALHOST] Authentication failed (user not found) for: " + militaryId);
                            return "AUTH_FAILURE&&IDENTIFIANT MILITAIRE INCORRECT";

              }

            }

    public static List<Contact> searchUsers(String query, String currentUserPhone) throws SQLException {
        List<Contact> results = new ArrayList<>();
        String sql = "SELECT * FROM contacts WHERE (nick_name LIKE ? OR contact_user_id LIKE ?) AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String likeQuery = "%" + query + "%";
            stmt.setString(1, likeQuery);
            stmt.setString(2, likeQuery);
            stmt.setString(3, currentUserPhone); // on exclut l'utilisateur courant

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(new Contact(rs.getString("contact_user_id"),
                        rs.getString("nick_name"),
                        rs.getString("lastMessage"),
                        null,
                        0,
                        null));
            }
        }

        return results;
    }


     public static User getUser(String userId) throws SQLException{
                String sql = "SELECT phone_number, grade, division, username FROM users WHERE phone_number= ?";
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                  stmt.setString(1, userId);

                  String msg = null;
                  ResultSet rs = stmt.executeQuery();

                 if (rs.next()) {
                                    System.out.println("[LOCALHOST] getting success (user found) for: " + userId);

                                    return new User( rs.getString("phone_number"), rs.getString("grade"),
                                    rs.getString("division"),  rs.getString("username"));

                  } else {
                                        System.out.println("[LOCALHOST] getting FAILED (user not found) for: " + userId);
                                        return null;


                 }

            }

        @SuppressWarnings("CallToPrintStackTrace")
     public static void addUser(User user) {
                String sql = "INSERT INTO users (phone_number, password_hash, grade, division, profilePicture, username) VALUES (?, ?, ?, ?, ?, ?)";

                try(Connection conn = Database.getConnection()){
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, user.getPhone_Number());
                    stmt.setString(2, user.getPasswordHash());
                    stmt.setString(3, user.getGrade());
                    stmt.setString(4, user.getDivision());
                    stmt.setString(5, "user.getProfilePicture()");
                    stmt.setString(6, user.getUsername());

                    stmt.executeUpdate();
                    System.out.println("[SERVER] User added: " + user.getPhone_Number());

                } catch (SQLException e) {
                    System.err.println("[SERVER] Error adding user: " + e.getMessage());
                    System.out.println(e.getMessage() +" : " +e.getCause());
                }
            }

     public static void saveMessage(Message message){
        String sql = "INSERT INTO messages (sender_id, receiver_id,content,timestamp,ack,statut,type) VALUES  (?,?,?,?,?,?,?)";
         try(Connection conn = Database.getConnection()){
             PreparedStatement stmt = conn.prepareStatement(sql);
             stmt.setString(1, message.getSender());
             stmt.setString(2, message.getRecipient());
             stmt.setString(3, message.getContent());
             // Convertir le timestamp en chaîne de caractères
             String timestampStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(message.getTimestamp());
             stmt.setString(4, timestampStr);
             stmt.setString(5, message.getAck());
             stmt.setString(6, message.getStatut());
             stmt.setString(7, message.getType());
            

             stmt.executeUpdate();
             System.out.println("[SERVER] SAVE SUCCCES MESSAGE: " + message.toString() +" FROM "+message.getSender()+ "TO "+message.getRecipient());

         } catch (SQLException e) {
             System.err.println("[SERVER] SAVE ERROR MESSAGE: " + message.toString() +" FROM "+message.getSender()+ "TO "+message.getRecipient());
             System.out.println(e.getMessage() +" : " +e.getCause());
         }
     }
    public static void saveFile(Message message){
        String sql = "INSERT INTO messages (sender_id, receiver_id,fileName,filePath,timestamp,ack,statut,type) VALUES  (?,?,?,?,?,?,?,?)";
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, message.getSender());
            stmt.setString(2, message.getRecipient());
            stmt.setString(3, message.getFileName());
            stmt.setString(4, message.getFilePath());
            // Convertir le timestamp en chaîne de caractères
            String timestampStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(message.getTimestamp());
            stmt.setString(5, timestampStr);
            stmt.setString(6, message.getAck());
            stmt.setString(7, message.getStatut());
            stmt.setString(8, message.getType());


            stmt.executeUpdate();
            System.out.println("[SERVER] SAVE SUCCCES FILE: " + message.toString() +" FROM "+message.getSender()+ " TO "+message.getRecipient());

        } catch (SQLException e) {
            System.err.println("[SERVER] SAVE ERROR FILE: " + message.toString() +" FROM "+message.getSender()+ "TO "+message.getRecipient());
            System.out.println(e.getMessage() +" : " +e.getCause());
        }
    }

     public static List<Contact> getContactsWithMessages(String userPhone) throws SQLException {
            String sql = "SELECT DISTINCT c.* FROM contacts c " +
                    "JOIN messages m ON (c.user_id = m.sender_id OR c.user_id = m.receiver_id) " +
                    "WHERE m.sender_id = ? OR m.receiver_id = ?";
            Connection connection = Database.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, userPhone);
            stmt.setString(2, userPhone);
                    ResultSet rs = stmt.executeQuery();

            List<Contact> contacts = new ArrayList<>();
            while (rs.next()) {
                contacts.add(new Contact(
                        rs.getString("contact_user_id"),
                        rs.getString("nick_name"),
                        rs.getString("lastMessage"),
                        rs.getString("created_at"),
                        0,
                        new Image(Objects.requireNonNull(Database.class.getResource("/com/alaanya/view/images/profile.png")).toExternalForm())));
            }
            return contacts;
        }

        public static List<Contact> getAllContacts() throws SQLException {
            Connection connection = Database.getConnection();
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM contacts");
            List<Contact> contacts = new ArrayList<>();
            while (rs.next()) {
                contacts.add(new Contact(
                        rs.getString("contact_user_id"),
                        rs.getString("nick_name"),
                        "",
                        "",
                        0,
                        new Image(Objects.requireNonNull(Database.class.getResource("/com/alaanya/view/images/profile.png")).toExternalForm())));
            }
            return contacts;
        }


    /**
     * Retrieves the conversation between two users from the database.
     * @param currentUserId The military ID of the current user.
     * @param contactId The military ID of the selected contact.
     * @return A list of Message objects representing the conversation.
     * @throws SQLException If a database error occurs.
     */
    public static List<Message> getChatDetails(String currentUserId, String contactId) {
        List<Message> conversation = new ArrayList<>();
        String sql = "SELECT * FROM messages " +
                "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                "ORDER BY timestamp";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentUserId);
            stmt.setString(2, contactId);
            stmt.setString(3, contactId);
            stmt.setString(4, currentUserId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                System.out.println(rs);
                //RECONSTTITU LES MESSAGES
                if (rs.getString("type").equals("FILE")) {
                    Message file = new Message(
                            rs.getString("sender_id"),
                            rs.getString("filePath"),
                            rs.getString("fileName"),
                            rs.getString("receiver_id"),
                            0
                    );
                    file.setAck(rs.getString("ack"));
                    file.setStatut(rs.getString("statut"));
                    file.setTimestamp(rs.getTimestamp("timestamp"));


                    conversation.add(file);

                } else {
                   Message message = new Message(
                           rs.getString("sender_id"),
                           "MESSAGE",
                           rs.getString("content"),
                           rs.getString("receiver_id")
                   );
                    message.setAck(rs.getString("ack"));
                    message.setStatut(rs.getString("statut"));
                    message.setTimestamp(rs.getTimestamp("timestamp"));
                    conversation.add(message);

                }


            }
        } catch (SQLException e) {
        System.err.println("[SERVER] GET CHAT DETAILS ERROR MESSAGE: ");
        System.out.println(e.getMessage() +" : " +e.getCause());
    }
        return conversation;
    }

    public static void updateMessageStatus(Message message, String read) throws SQLException {
        String sql = "UPDATE messages SET statut = ? WHERE sender_id = ? AND content = ?";
        Connection connection = Database.getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql);

            stmt.setString(1, read);
            stmt.setString(2, message.getSender());
            if( message.getType().equals("MESSAGE")) stmt.setString(3, message.getContent());
            else stmt.setString(3, message.getFileName());
            stmt.executeUpdate();
    }

    public static void updateContact(Message message) throws SQLException {
        String sql = """
                UPDATE contacts
                SET lastMessage = ?, created_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND contact_user_id = ?;
                """;
        Connection connection = Database.getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql);

        if( message.getType().equals("MESSAGE")) stmt.setString(1, message.getContent());
        else stmt.setString(1, message.getFileName());

        stmt.setString(2, message.getSender());
        stmt.setString(3, message.getRecipient());

        stmt.executeUpdate();
    }
}

