package com.alaanya.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import com.alaanya.database.Database;
public class User {

    private String militaryId;
    private String passwordHash;
    private String grade;
    private String division;
    private int clearanceLevel;
    private boolean twoFactorEnabled;
    private String sessionToken;
    private String lastLogin;
    private String username;


    public User(String militaryId, String grade, String division, int clearanceLevel, String username) {
        this.militaryId = militaryId;
        this.grade = grade;
        this.division = division;
        this.clearanceLevel = clearanceLevel;
        this.username = username;
    }

    public static boolean exists(String militaryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE military_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, militaryId);
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
    public String getMilitaryId() {
        return militaryId;
    }

    public void setMilitaryId(String militaryId) {
        if (!militaryId.matches("[A-Z]{2}-\\d{5}")) {
            throw new IllegalArgumentException("Format d'ID militaire invalide (AA-12345)");
        }
        this.militaryId = militaryId;
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

    public int getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(int clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
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
}
