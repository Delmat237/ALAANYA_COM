package server.model;


import java.io.Serializable;

public class Notification implements Serializable {
    private String type; // Type de notification (alerte, message prioritaire, etc.)
    private String message; // Message de la notification
    private int priority; // Priorité de la notification
    private String sender; // Expéditeur de la notification
    private static final long serialVersionUID = 1L;

    public Notification(String type, String message, int priority, String sender) {
        this.type = type;
        this.message = message;
        this.priority = priority;
        this.sender = sender;
    }

    public Notification(String authSuccess, String message) {
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getPriority() {
        return priority;
    }

    public String getSender() {
        return sender;
    }
}
