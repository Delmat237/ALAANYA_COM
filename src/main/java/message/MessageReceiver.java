package message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Message;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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
                    Message msg = gson.fromJson(json, Message.class);
                    System.out.println("[" + msg.getTimestamp() + "] " + msg.getSender() + " : " + msg.getContent());

                    if (listener != null) {
                        listener.onMessageReceived(msg);
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
