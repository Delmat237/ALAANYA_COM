package com.alaanya.socket;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

public class FileMessage extends Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;
    private byte[] fileData;
    private String fileName;
    private long fileSize;  // Optional: Useful for progress updates
    private String recipientID;

    public FileMessage(String sender, String type, String content, String fileName, byte[] fileData,String recipientID) {
        super(sender, type, content,recipientID);
        this.fileData = fileData;
        this.fileName = fileName;
        this.fileSize = fileData.length; // Set file size upon creation
        this.recipientID = recipientID;

    }

    public byte[] getFileData() {
        return fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getRecipient(){
        return recipientID;
    }

    // Override toString() for debugging purposes
    @Override
    public String toString() {
        return "FileMessage{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}
