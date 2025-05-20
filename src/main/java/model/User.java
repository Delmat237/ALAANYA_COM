package model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import database.Database;
public class User {

    private String phone_Number; //numero telephone
    private String passwordHash;
    private String grade;
    private String division;
    private boolean twoFactorEnabled;
    private String sessionToken;
    private String lastLogin;
    private String username;
    private String profilePicture;


    public User(String phone_Number, String grade, String division,String username) {
        this.phone_Number = phone_Number;
        this.grade = grade;
        this.division = division;
        this.username = username;
    }

    public static boolean exists(String phone_Number) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE phone_number = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone_Number);
            ResultSet rs = stmt.executeQuery();

            if(rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }
    // Méthode de hachage SHA-256 pour le mot de passe
    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    // Getters et Setters
    public String getPhone_Number() {
        return phone_Number;
    }

    public void setphone_Number(String phone_Number) {
        // if (!phone_Number.matches("[A-Z]{2}-\\d{5}")) {
        //     throw new IllegalArgumentException("Format d'ID militaire invalide (AA-12345)");
        // }
        this.phone_Number = phone_Number;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String password) throws NoSuchAlgorithmException {
        //this.passwordHash = hashPassword(password);
        this.passwordHash = password;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }


    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString(){
        return "User{" +
                "phone_NUmber='" + phone_Number + '\'' +
                ", passwordHard='" + passwordHash + '\'' +
                ", Username='" + username + '\'' +
                ", Grade='" + grade + '\'' +
                ", Division=" + division +
                '}';
    }

    public String getProfilePicture() {
        return this.profilePicture;
    }
}
