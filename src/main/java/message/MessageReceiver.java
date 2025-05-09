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

    public MessageReceiver(int port) {
        this.port = port;
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
                    System.out.println("[Receiver] Connexion reçue depuis " + clientSocket.getInetAddress());

                    String json = dis.readUTF();
                    System.out.println("[Receiver] JSON reçu : " + json);

                    Message msg = gson.fromJson(json, Message.class);
                    System.out.println("[" + msg.getTimestamp() + "] " + msg.getSender() + " : " + msg.getContent());

                } catch (IOException e) {
                    System.err.println("[Receiver] Erreur réception message : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("[Receiver] Erreur serveur socket : " + e.getMessage());
            e.printStackTrace();
        }
    }

}
