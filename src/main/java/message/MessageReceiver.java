package message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controller.MessageController;
import controller.MainController;
import database.Database;
import model.Message;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class MessageReceiver extends Thread {
    private final int port;
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    private MessageListener listener;

    public MessageReceiver(int port) {
        this.port = port;
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Receiver] En attente de messages sur le port " + port);
            while (true) {
                try (
                        Socket clientSocket = serverSocket.accept();
                        DataInputStream dis = new DataInputStream(clientSocket.getInputStream())
                ) {
                    String json = dis.readUTF();
                    Message message = gson.fromJson(json, Message.class);
                    System.out.println("[" + message.getTimestamp() + "] " + message.getSender() + " : " + message.getContent());
                    //change le statut du message en receive
                    message.setAck("receive");
                    //note comme non lu
                    message.setStatut("notread");
                    
                    //Sauvegarde su ce n'est pas un accusé de reception
                    if (!Objects.equals(message.getType(), "ACK_READ"))
                        Database.saveMessage(message);
                    if (listener != null) {
                        listener.onMessageReceived(message);
                    }
                } catch (IOException e) {
                    System.err.println("[Receiver] Erreur réception message : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    public interface MessageListener {
        void onMessageReceived(Message message);
    }

}
