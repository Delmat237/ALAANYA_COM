package database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

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
                    e.printStackTrace();
                }
            }
        }

        // Créer la base de données et la table si elles n'existent pas
        try {
            createDatabaseAndTable();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la base de données et de la table : " + e.getMessage());
            e.printStackTrace();
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
                         profilePicture TEXT,
                         username VARCHAR(255) NOT NULL
                     );""");
    
            // Création table contacts
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contacts (
                    id  INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id VARCHAR(15) NOT NULL,
                    contact_user_id VARCHAR(15) NOT NULL,
                    nick_name TEXT NOT NULL,
                    statut INTEGER CHECK(statut IN (0, 1)) DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(phone_Number),
                 
                    UNIQUE(user_id, contact_user_id)
                )""");
    
            // Création table messages
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_id VARCHAR(15) NOT NULL,
                    receiver_id VARCHAR(15) NOT NULL,
                    content TEXT NOT NULL,
                    timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
                    statut TEXT CHECK(statut IN ('sent', 'delivered', 'read')) DEFAULT 'sent',
                    FOREIGN KEY (sender_id) REFERENCES users(phone_Number)
                   
                )""");
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
       e.printStackTrace();
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

    public static List<String> getContacts(String militaryId, List<String> contactsList) throws SQLException {
        System.out.println("Recherche des contacts");

        String sql = "SELECT contact_user_id, nick_name from contacts where user_id =?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, militaryId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("nick_name");

                String contactId = rs.getString("contact_user_id");

                String contact = username + " (" + contactId + ")";
                contactsList.add(contact); // ajout à la liste
                System.out.println(contact);


            }
            return contactsList;

        }
    }


    public static User getUser(String militaryId) throws SQLException{
            String sql = "SELECT phone_number, grade, division, clearance_level, username FROM users WHERE phone_number= ?";
            Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
              stmt.setString(1, militaryId);
            
              String msg = null;
              ResultSet rs = stmt.executeQuery();

             if (rs.next()) {
                                System.out.println("[LOCALHOST] getting success (user found) for: " + militaryId);
                                
                                return new User( rs.getString("phone_number"), rs.getString("grade"), 
                                rs.getString("division"),  rs.getString("username"));
                                  
              } else {
                                    System.out.println("[LOCALHOST] getting FAILED (user not found) for: " + militaryId);
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
                e.printStackTrace();
            }
        }

}
