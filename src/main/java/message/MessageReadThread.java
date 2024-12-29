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
            while((message = in.readUTF()) != null ){
                System.out.println(message);

                // Format attendu : ID_EXPEDITEUR&&MESSAGE
                String[] parts = message.split("&&");
                this.chatApp.senderId = parts[0];
                String messageContent = parts[1];

                if ("FILE".equals(messageContent)){
                    new FileReceicerThread(this.chatApp,this.socket).start();
                }else{
                    System.out.println(this.chatApp.senderId +":"+messageContent);
                    this.chatApp.showInfo("New message","message from "+this.chatApp.senderId );
                    //Ajout du message dans l'app
                    Platform.runLater(() -> this.chatApp.addMessage(this.chatApp.getChatBox(this.chatApp.senderId),
                            this.chatApp.senderId,messageContent,false));

                }
                }
        }catch (IOException e) {
        this.chatApp.showError("Lecture impossible: " + e.getMessage());
    }
    }
}