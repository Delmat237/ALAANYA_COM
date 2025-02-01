package file;

import alaanya.ChatApp;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class FileReceicerThread extends Thread{
    private Socket socket;
    private ChatApp chatApp;
    private DataInputStream in;

    public FileReceicerThread(ChatApp chatApp, Socket socket) throws IOException {
        this.socket = socket;
        this.chatApp = chatApp;
        in = new DataInputStream(this.socket.getInputStream());
    }

    @Override
    public void run() {
        //RECEPTION DES METADONNÉES

        String filename = null; // Lire le nom du fichier
        long totalFileSize = 0; // Lire la taille totale du fichier


        try {
            filename = in.readUTF();

            totalFileSize = in.readLong();
        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture des metadonnées");;
        }

        System.out.println("Réception du fichier : " + filename + " (" + totalFileSize + " octets)");

        File receivedFile = new File("Downloads/" + filename);
        receivedFile.getParentFile().mkdirs(); // Crée le répertoire Downloads si nécessaire

        try (FileOutputStream fileOutputStream = new FileOutputStream(receivedFile)) {

            metadata(totalFileSize, receivedFile, fileOutputStream, in);
            // Ajouter le fichier reçu au chat

            String finalFilename = filename;

            Platform.runLater(() ->this.chatApp.addFile(this.chatApp.contactChats.get(this.chatApp.senderId).BoxGetter(), "/icons/document-signed.png", finalFilename, false));
            System.out.println("Fichier recu avec succès : " + finalFilename);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void metadata(long totalFileSize, File receivedFile, FileOutputStream fileOutputStream, DataInputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        long bytesReadTotal = 0;

        while (bytesReadTotal < totalFileSize) {
            System.out.println((bytesReadTotal / totalFileSize)*100);
            int bytesToRead = (int) Math.min(buffer.length, totalFileSize - bytesReadTotal);

            int bytesRead = in.read(buffer, 0, bytesToRead);

            if (bytesRead == -1) {
                throw new IOException("Fin inattendue du flux pendant la réception du fichier.");
            }

            fileOutputStream.write(buffer, 0, bytesRead);
            bytesReadTotal += bytesRead;
        }

        System.out.println("Fichier reçu complètement : " + receivedFile.getAbsolutePath());
    }


}
