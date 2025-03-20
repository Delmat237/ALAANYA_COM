package com.alaanya.socket;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String sender;
    private String type;
    private String content;
    private String recipient;
    private Date timestamp;  // Add the timestamp field

    public Message(String sender, String type, String content) {
        this(sender, type, content, null); // Call the all-args constructor
    }

    public Message(String sender, String type, String content, String recipient) {
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.recipient = recipient;
        this.timestamp = new Date(); // Initialize the timestamp
    }

    public Message(String sender, String type, String content, String recipient, Date timestamp) {
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.recipient = recipient;
        this.timestamp = timestamp; //  timestamp is passed in
    }

    public String getSender() {
        return sender;
    }

    public String getType() {
        return type;
    }


    public String getContent() {
        return content;
    }

    public String getRecipient() {
        return recipient;
    }

    public Date getTimestamp() {  // Add the getter for the timestamp
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", recipient='" + recipient + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
