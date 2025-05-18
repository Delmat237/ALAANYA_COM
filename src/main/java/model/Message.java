package model;

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
    private long filesize;
    private String fileName;
    private String filePath;
    private Date timestamp;  // Add the timestamp field
    private String ack; //sent;receive
    private String statut;//read or notread

    public Message(){}

    public Message(String sender, String type, String content, String recipient) {
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.recipient = recipient;
        this.timestamp = new Date();
    }

    //Format des fichiers attaché à un fichier
    public Message(String sender,String filePath, String fileName, String recipient, long filesize ) {
        this.sender = sender;
        this.type = "FILE";
        this.filePath = filePath;
        this.fileName = fileName;
        this.recipient = recipient;
        this.filesize = filesize;
        this.timestamp = new Date();
    }

    public String getSender() {
        return this.sender;
    }

    public String getType() {
        return this.type;
    }
    public String getAck(){return this.ack;}
    public void setAck(String ack){this.ack = ack;}

    public String getContent() {
        return this.content;
    }

    public String getRecipient() {
        return this.recipient;
    }

    public Date getTimestamp() {  // Add the getter for the timestamp
        return this.timestamp;
    }

    public void setTimestamp(Date date) { this.timestamp = date;}
    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public long getFileSize() {
        return this.filesize;
    }
    public String getStatut() {return this.statut;}

    public void setStatut(String statut) {this.statut = statut;}
    public void setSender(String sender) {this.sender = sender;}
    public void setRecipient(String recipient) {this.recipient = recipient;}
    public void setContent(String content) {this.content = content;}
    public void setType(String type) {this.type = type;}


    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", recipient='" + recipient + '\'' +
                ", timestamp=" + timestamp +
                ",fileName = "+fileName +

                '}';
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
