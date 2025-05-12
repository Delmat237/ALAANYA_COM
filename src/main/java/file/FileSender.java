package file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import database.Database;
import model.Message;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class FileSender extends Thread {

    private final String ip;
    private final int port;
    private final File file;
    private final String sender;
    private final String recipient;
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    public FileSender(String ip, int port, File file, String sender, String recipient) {
        this.ip = ip;
        this.port = port;
        this.file = file;
        this.sender = sender;
        this.recipient = recipient;
    }

    @Override
    public void run() {
        try (
                Socket socket = new Socket(ip, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream(file)
        ) {
            // Étape 1 : Envoyer un message JSON avec les métadonnées
            Message message = new Message(sender, "FILE", file.getName(), recipient, file.length(),new Date());
            message.setAck("sent");

            //Affiche


            //save in bd
            Database.saveMessage(message);

            String json = gson.toJson(message);
            dos.writeUTF(json); // envoie du JSON

            // Étape 2 : Envoyer le fichier binaire
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }

            System.out.println("Fichier envoyé : " + file.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
