package message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MessageSender extends Thread {
    private final String ip;
    private final int port;
    private final Message message;
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    public MessageSender(String ip, int port, Message message) {
        this.ip = ip;
        this.port = port;
        this.message = message;
    }

    @Override
    public void run() {
        try (
                Socket socket = new Socket(ip, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream())
        ) {
            String json = gson.toJson(message);
            dos.writeUTF(json);
            System.out.println("Message envoyé : " + message.getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
