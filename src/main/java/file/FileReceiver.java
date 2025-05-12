package file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import database.Database;
import message.MessageReceiver;
import model.Message;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileReceiver extends Thread {

    private final int port;
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    private MessageReceiver.MessageListener listener;
    
    public FileReceiver(int port) {
        this.port = port;
    }

    public void setFileListener(MessageReceiver.MessageListener listener) {
        this.listener = listener;
    }
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("En attente de fichiers sur le port " + port);
            while (true) {
                try (
                        Socket clientSocket = serverSocket.accept();
                        DataInputStream dis = new DataInputStream(clientSocket.getInputStream())
                ) {
                    // Étape 1 : Lire le JSON
                    String json = dis.readUTF();
                    Message message = gson.fromJson(json, Message.class);
                    //change le statut du message en receive
                    message.setAck("receive");
                    //note comme non lu
                    message.setStatut("notread");

                    //Save in bd
                    Database.saveMessage(message);

                    if (listener != null) {
                        listener.onMessageReceived(message);
                    }

                    if ("FILE".equals(message.getType())) {
                        //CReation d'un dossier
                        File dir = new File("Downloads");
                        if (!dir.exists()) dir.mkdirs();
                        String filename = System.currentTimeMillis() + "_received_" + message.getContent();
                        File outFile = new File(dir, filename);


                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[4096];
                            long remaining = message.getFileSize();
                            int bytesRead;
                            while (remaining > 0 &&
                                    (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                remaining -= bytesRead;
                            }
                        }

                        System.out.println("Fichier reçu : " + outFile.getName());
                    }

                } catch (IOException e) {
                    System.err.println("Erreur réception : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
