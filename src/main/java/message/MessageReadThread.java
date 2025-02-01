package message;

import alaanya.ChatApp;
import file.FileReceicerThread;
import file.FileSendThread;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class MessageReadThread extends Thread {

    private final Socket socket;
    private final ChatApp chatApp;

    public MessageReadThread(Socket socket, ChatApp chatApp) {
        this.socket = socket;
        this.chatApp = chatApp;
    }

    @Override
    public void run() {
        try{
            DataInputStream in = new DataInputStream(this.socket.getInputStream());
            String message;
            while((message = in.readUTF()) != null ) {

                // Format attendu : ID_EXPEDITEUR&&MESSAGE
                String[] parts = message.split("&&");

                if (parts[0].equals("STATUS")) {
                    boolean status = parts[1].equals("1") ? true : false;
                    String contactId = parts[2];
                    this.chatApp.contactChats.compute(
                            contactId, (k, contact) -> new ChatApp.Contact(contact.GetIP(), contact.GetName(), contact.BoxGetter(), status));

                } else {
                    this.chatApp.senderId = parts[0];
                    String messageContent = parts[1];


                    if (!this.chatApp.senderId.equals(this.chatApp.currentContactId)) {
                        this.chatApp.showInfo("New message", "message from " + this.chatApp.senderId);
                    }

                    //Ajout du message dans l'app
                    Platform.runLater(() -> this.chatApp.addMessage(this.chatApp.contactChats.get(this.chatApp.senderId).BoxGetter(),
                            this.chatApp.senderId, messageContent, false));


                }
            }
        }catch (IOException e) {
        this.chatApp.showError("Lecture impossible: " + e.getMessage());
    }
    }
}
